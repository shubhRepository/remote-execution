package com.remote.consumer.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;
import com.remote.consumer.config.Constants;
import com.remote.consumer.model.CodeSubmission;
import com.remote.consumer.event.ContainerOutputEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;


@Service
public class DockerService {

    private static final Logger log = LoggerFactory.getLogger(DockerService.class);

    private final DockerClient dockerClient;
    private final ApplicationEventPublisher eventPublisher;

    // Store container input streams for each session
    private final ConcurrentHashMap<String, PipedOutputStream> containerInputStreams = new ConcurrentHashMap<>();

    @Autowired
    public DockerService(DockerClient dockerClient, ApplicationEventPublisher eventPublisher) {
        this.dockerClient = dockerClient;
        this.eventPublisher = eventPublisher;
    }

    public byte[] executeCode(CodeSubmission codeSubmission) throws InterruptedException {
        String containerId = createContainer(codeSubmission);
        return runInteractiveContainer(containerId, codeSubmission.getSessionId());
    }

    public void sendInputToContainer(String sessionId, String input) {
        PipedOutputStream inputStream = containerInputStreams.get(sessionId);
        if (inputStream != null) {
            try {
                if (!input.endsWith("\n")) {
                    input += "\n";
                }
                inputStream.write(input.getBytes());
                inputStream.flush();
                log.info("Input sent to container for session {}: {}", sessionId, input.trim());
            } catch (IOException e) {
                log.error("Error sending input to container for session {}", sessionId, e);
            }
        } else {
            log.warn("No active container found for session {}", sessionId);
        }
    }

    public void closeInputForSession(String sessionId) {
        PipedOutputStream inputStream = containerInputStreams.remove(sessionId);
        if (inputStream != null) {
            try {
                inputStream.close();
                log.info("Input stream closed for session {}", sessionId);
            } catch (IOException e) {
                log.error("Error closing input stream for session {}", sessionId, e);
            }
        }
    }

    private String createContainer(CodeSubmission codeSubmission) throws InterruptedException {
        String imageName = "python:3.9";
        boolean exists = isImageExists(imageName);

        if (!exists) {
            pullDockerImage(imageName);
        }

        String decodedCode = new String(Base64.getDecoder().decode(codeSubmission.getCodeContent().getBytes()));

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withName(Constants.CONTAINER_NAMESPACE + codeSubmission.getSessionId())
                .withCmd("python", "-c", decodedCode)
                .withHostConfig(HostConfig.newHostConfig().withAutoRemove(true))
                .withStdinOpen(true) // Enable stdin
                .withAttachStdin(true) // Attach stdin
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(false)
                .exec();

        return container.getId();
    }

    private byte[] runInteractiveContainer(String containerId, String sessionId) throws InterruptedException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CountDownLatch containerFinished = new CountDownLatch(1);
        StringBuilder outputBuffer = new StringBuilder();

        try {
            PipedInputStream containerInput = new PipedInputStream(8192); // Larger buffer
            PipedOutputStream inputToContainer = new PipedOutputStream(containerInput);

            containerInputStreams.put(sessionId, inputToContainer);

            dockerClient.startContainerCmd(containerId).exec();

            dockerClient.attachContainerCmd(containerId)
                    .withStdIn(containerInput)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback.Adapter<Frame>() {

                        @Override
                        public void onNext(Frame frame) {
                            try {
                                byte[] payload = frame.getPayload();
                                String message = new String(payload);
                                StreamType streamType = frame.getStreamType();

                                synchronized (outputBuffer) {
                                    outputBuffer.append(message);

                                    String bufferedContent = outputBuffer.toString();
                                    if (bufferedContent.contains("\n") || bufferedContent.contains(";")) {
                                        log.info("Container output [{}]: '{}'", streamType, bufferedContent.replaceAll("\\s+", " "));

                                        eventPublisher.publishEvent(new ContainerOutputEvent(sessionId, bufferedContent));
                                        outputStream.write(bufferedContent.getBytes());
                                        outputBuffer.setLength(0);
                                    }
                                }

                            } catch (Exception e) {
                                log.error("Error processing container output", e);
                            }
                        }

                        @Override
                        public void onComplete() {
                            log.info("Container execution completed for session {}", sessionId);

                            // Send any remaining buffered output
                            synchronized (outputBuffer) {
                                if (!outputBuffer.isEmpty()) {
                                    String remainingContent = outputBuffer.toString();
                                    log.info("Sending remaining output: '{}'", remainingContent);
                                    eventPublisher.publishEvent(new ContainerOutputEvent(sessionId, remainingContent));
                                    outputBuffer.setLength(0);
                                }
                            }

                            containerFinished.countDown();
                            closeInputForSession(sessionId);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            log.error("Error in container execution for session {}: {}", sessionId, throwable.getMessage());

                            // Send any remaining buffered output before closing
                            synchronized (outputBuffer) {
                                if (!outputBuffer.isEmpty()) {
                                    String remainingContent = outputBuffer.toString();
                                    eventPublisher.publishEvent(new ContainerOutputEvent(sessionId, remainingContent));
                                    outputBuffer.setLength(0);
                                }
                            }

                            containerFinished.countDown();
                            closeInputForSession(sessionId);
                        }
                    });

            boolean completed = containerFinished.await(300, java.util.concurrent.TimeUnit.SECONDS);
            if (!completed) {
                log.warn("Container execution timed out for session {}", sessionId);
            }

        } catch (IOException e) {
            log.error("Error setting up interactive container", e);
            closeInputForSession(sessionId);
        } finally {
            // Ensure cleanup
            closeInputForSession(sessionId);
        }

        return outputStream.toByteArray();
    }

    private boolean isImageExists(String imageName) {
        return dockerClient.listImagesCmd()
                .exec()
                .stream()
                .anyMatch(img -> {
                    if (img.getRepoTags() != null) {
                        for (String tag : img.getRepoTags()) {
                            if (tag.equals(imageName)) {
                                return true;
                            }
                        }
                    }
                    return false;
                });
    }

    private void pullDockerImage(String imageName) throws InterruptedException {
        dockerClient.pullImageCmd(imageName)
                .exec(new PullImageResultCallback())
                .awaitCompletion();
    }
}