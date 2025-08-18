package com.remote.consumer.config;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Duration;

@Configuration
public class DockerConfigHttpClient5 {

    private static final Logger log = LoggerFactory.getLogger(DockerConfigHttpClient5.class);

    @Bean
    @ConditionalOnProperty(name = "docker.enabled", havingValue = "true", matchIfMissing = true)
    public DockerClient dockerClient() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(Constants.DOCKER_HOST)
                .withDockerTlsVerify(false)
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .connectionTimeout(Duration.ofSeconds(5))
                .responseTimeout(Duration.ofSeconds(30))
                .maxConnections(100)
                .build();

        return DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();
    }


    // sanity check
    @Bean
    @ConditionalOnProperty(name = "docker.enabled", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner verifyDocker(DockerClient client) {
        return args ->
            log.info("Docker version: {}", client.versionCmd().exec().getVersion());
    }
}