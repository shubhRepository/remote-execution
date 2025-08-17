package com.remote.consumer;

import com.github.dockerjava.api.DockerClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConsumerServiceApplication {

    @Autowired
    DockerClient dockerClient;

	public static void main(String[] args) {
        SpringApplication.run(ConsumerServiceApplication.class, args);
	}

    @PostConstruct
    public void testDocker() {
        System.out.println("Docker version: " + dockerClient.versionCmd().exec().getVersion());
    }

}
