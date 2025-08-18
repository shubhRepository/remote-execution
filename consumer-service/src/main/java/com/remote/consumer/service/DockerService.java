package com.remote.consumer.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.remote.consumer.model.CodeSubmission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;


@Service
public class DockerService {

    private final DockerClient dockerClient;

    @Autowired
    public DockerService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public String createContainer(CodeSubmission codeSubmission) throws RuntimeException {
        String imageName = "python:3.9";
        boolean exists = isImageExists(imageName);

        if (!exists) {
            pullDockerImage(imageName);
        }

        String decodedCode = new String(Base64.getDecoder().decode(codeSubmission.getCodeContent().getBytes()));
        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withName("code-execution-" + codeSubmission.getSessionId())
                .withCmd("python", "-c", decodedCode)
                .exec();

        runContainer(container.getId());

        return container.getId();
    }

    private void runContainer(String containerId) {
        dockerClient.startContainerCmd(containerId).exec();
        try {
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            byte[] payload = frame.getPayload();
                            String message = new String(payload);
                            System.out.println("Message: " + message);
                        }
                    })
                    .awaitCompletion();
        } catch (InterruptedException e) {
            System.out.println("Container execution interrupted" + e);
            Thread.currentThread().interrupt();
        }
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

    private void pullDockerImage(String imageName) {
        try {
            dockerClient.pullImageCmd(imageName)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to pull image " + imageName, e);
        }
    }
}