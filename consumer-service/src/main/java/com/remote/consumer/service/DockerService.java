package com.remote.consumer.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
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

    public void listAllContainers() {
        System.out.println("--- Listing all containers (running and stopped) ---");
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (Container container : containers) {
            System.out.println("Container ID: " + container.getId() + ", Name: " + container.getNames()[0] + ", Status: " + container.getStatus());
        }
        System.out.println("-------------------------------------------------");
    }
}