package com.remote.consumer.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.remote.consumer.config.Constants;
import com.remote.consumer.model.CodeSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;


@Service
public class DockerService {

    private static final Logger log = LoggerFactory.getLogger(DockerService.class);

    private final DockerClient dockerClient;

    @Autowired
    public DockerService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public byte[] executeCode(CodeSubmission codeSubmission) throws InterruptedException {
//        String containerId = null;
//        try {
//            containerId = createContainer(codeSubmission);
//            return runContainer(containerId);
//        } finally {
//            if (containerId != null) {
//                removeContainer(containerId);
//            }
//        }
        String containerId = createContainer(codeSubmission);
        return runContainer(containerId);
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
                .exec();

        return container.getId();
    }

    private byte[] runContainer(String containerId) throws InterruptedException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        dockerClient.startContainerCmd(containerId).exec();
        dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(new ResultCallback.Adapter<Frame>() {
                    @Override
                    public void onNext(Frame frame) {
                        byte[] payload = frame.getPayload();
                        String message = new String(payload);
                        log.info("Message: {}", message);
                        try {
                            outputStream.write(payload);
                        } catch (IOException e) {
                            log.error("Error writing to output stream", e);
                        }
                    }
                })
                .awaitCompletion();
        return outputStream.toByteArray();
    }

//    private void removeContainer(String containerId) {
//        dockerClient.removeContainerCmd(containerId)
//                .withForce(true)
//                .withRemoveVolumes(true)
//                .exec();
//        log.info("Container {} removed successfully", containerId);
//    }

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