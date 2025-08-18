package com.remote.consumer.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PullResponseItem;
import com.remote.consumer.model.CodeSubmission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withName("code-execution-" + codeSubmission.getSessionId())
                .withCmd(new String[] { "python", "-c", codeSubmission.getCodeContent() })
                .exec();

        return container.getId();
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