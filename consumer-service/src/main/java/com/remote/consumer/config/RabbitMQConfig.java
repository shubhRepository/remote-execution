package com.remote.consumer.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "file-execution-queue";

    @Bean
    public Queue fileExecutionQueue() {
        return new Queue(QUEUE_NAME, true); // The 'true' makes the queue durable
    }
}
