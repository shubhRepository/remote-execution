package com.remote.consumer.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Create a queue if not exists
    @Bean
    public Queue fileExecutionQueue() {
        return new Queue(Constants.FILE_EXECUTION_QUEUE, true); // The 'true' makes the queue durable
    }
}
