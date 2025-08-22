package com.remote.consumer.listener;

import com.remote.consumer.service.DockerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.remote.consumer.config.Constants;
import com.remote.consumer.model.CodeSubmission;

@Service
public class QueueListener {

    private static final Logger log = LoggerFactory.getLogger(QueueListener.class);

    private final DockerService dockerService;

    @Autowired
    public QueueListener(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @RabbitListener(queues = Constants.FILE_EXECUTION_QUEUE, concurrency = "1")
    public void receiveCodeExecution(CodeSubmission codeSubmission) throws InterruptedException {
        byte[] containerResponse = dockerService.executeCode(codeSubmission);
        log.info("Container response: {}", new String(containerResponse));
    }
}