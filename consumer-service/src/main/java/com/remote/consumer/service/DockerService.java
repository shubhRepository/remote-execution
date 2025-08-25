package com.remote.consumer.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import com.remote.consumer.model.CodeSubmission;
import com.remote.consumer.event.ContainerOutputEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private String fileName;

    @Autowired
    public DockerService(DockerClient dockerClient, ApplicationEventPublisher eventPublisher) {
        this.dockerClient = dockerClient;
        this.eventPublisher = eventPublisher;
    }

    public byte[] executeCode(CodeSubmission codeSubmission) throws InterruptedException, IOException {
        String containerId = createContainer(codeSubmission);
        return runInteractiveContainer(containerId, codeSubmission.getSessionId());
    }

    // Method to send input to a running container
    public void sendInputToContainer(String sessionId, String input) {
        log.info("Attempting to send input to session {}: '{}'. Active sessions: {}",
                sessionId, input.trim(), containerInputStreams.keySet());

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
                containerInputStreams.remove(sessionId);
            }
        } else {
            log.warn("No active container found for session {}. Active sessions: {}. " +
                            "This might happen if the container finished executing or hasn't started yet.",
                    sessionId, containerInputStreams.keySet());
        }
    }

    // Method to close input stream
    public void closeInputForSession(String sessionId) {
        log.info("Attempting to close input stream for session {}. Active sessions before: {}",
                sessionId, containerInputStreams.keySet());

        PipedOutputStream inputStream = containerInputStreams.remove(sessionId);
        if (inputStream != null) {
            try {
                inputStream.close();
                log.info("Input stream closed for session {}. Remaining active sessions: {}",
                        sessionId, containerInputStreams.keySet());
            } catch (IOException e) {
                log.error("Error closing input stream for session {}", sessionId, e);
            }
        } else {
            log.info("No input stream found to close for session {}. Active sessions: {}",
                    sessionId, containerInputStreams.keySet());
        }
    }

    private String createContainer(CodeSubmission codeSubmission) throws InterruptedException, IOException {
        String imageName = getImageName(codeSubmission.getLanguage());
        Path tempDir = mountCodeToTempDir(codeSubmission);
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withAutoRemove(true)
                .withBinds(new Bind(tempDir.toAbsolutePath().toString(), new Volume("/workspace")));

        String[] cmd = createExecutionCommand(codeSubmission.getLanguage().toLowerCase());

        boolean exists = isImageExists(imageName);

        if (!exists) {
            pullDockerImage(imageName);
        }

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withName("exec_" + codeSubmission.getSessionId())
                .withHostConfig(hostConfig)
                .withCmd(cmd)
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
            log.info("Container input stream created for session {}. Total active sessions: {}",
                    sessionId, containerInputStreams.size());
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
                                    if (bufferedContent.contains("\n") || bufferedContent.contains(":") || bufferedContent.length() > 50) {
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

    private String getImageName(String language) {
        String imageName = null;
        switch (language.toLowerCase()) {
            case "python", "py":
                imageName = "python:3.9";
                break;
            case "java":
                imageName = "openjdk:17";
                break;
            case "javascript", "js":
                imageName = "node:16";
                break;
            case "c", "cpp", "c++":
                imageName = "gcc:latest";
                break;
            default:
                break;
        }
        if (imageName == null)
            throw new IllegalArgumentException(String.format("%s language is not supported.", language));
        return imageName;
    }

    private Path mountCodeToTempDir(CodeSubmission codeSubmission) throws IOException {
        String decodedCode = new String(Base64.getDecoder().decode(codeSubmission.getCodeContent().getBytes()));

        Path tempDir = Files.createTempDirectory("code_exec_" + codeSubmission.getSessionId());
        String language = codeSubmission.getLanguage().toLowerCase();

        switch (language) {
            case "python", "py":
                this.fileName = "script.py";
                break;
            case "java":
                this.fileName = "Solution.java";
                break;
            case "javascript", "js":
                this.fileName = "script.js";
                break;
            case "c":
                this.fileName = "solution.c";
                break;
            case "cpp", "c++":
                this.fileName = "solution.cpp";
                break;
            default:
                throw new IllegalArgumentException(String.format("%s language is not supported.", language));
        }

        Path codeFile = tempDir.resolve(fileName);
        Files.writeString(codeFile, decodedCode);
        return tempDir;
    }

    private String[] createExecutionCommand(String language) {
        String[] cmd;
        switch (language) {
            case "python", "py":
                cmd = new String[]{"python", "/workspace/" + fileName};
                break;
            case "java":
                cmd = new String[]{"sh", "-c", "cd /workspace && javac " + fileName + " && java Solution"};
                break;
            case "javascript", "js":
                cmd = new String[]{"node", "/workspace/" + fileName};
                break;
            case "c":
                cmd = new String[]{"sh", "-c", "cd /workspace && gcc -o solution " + fileName + " && ./solution"};
                break;
            case "cpp", "c++":
                cmd = new String[]{"sh", "-c", "cd /workspace && g++ -o solution " + fileName + " && ./solution"};
                break;
            default:
                throw new IllegalArgumentException(String.format("%s language is not supported.", language));
        }
        log.info("Created execution command for {}: {}", language, String.join(" ", cmd));
        return cmd;
    }
}