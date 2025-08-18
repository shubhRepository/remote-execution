package com.remote.consumer.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.remote.consumer.config.Constants;
import com.remote.consumer.model.CodeSubmission;

@Service
public class ListenerService {

    private final DockerService dockerService;

    @Autowired
    public ListenerService(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @RabbitListener(queues = Constants.FILE_EXECUTION_QUEUE, concurrency = "1")
    public void receiveCodeExecution(CodeSubmission codeSubmission) throws InterruptedException {
        System.out.println(dockerService.createContainer(codeSubmission));
    }
}