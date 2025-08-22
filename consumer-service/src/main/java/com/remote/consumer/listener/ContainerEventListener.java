package com.remote.consumer.listener;

import com.remote.consumer.event.ContainerInputEvent;
import com.remote.consumer.event.ContainerOutputEvent;
import com.remote.consumer.service.DockerService;
import com.remote.consumer.component.WebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ContainerEventListener {

    private static final Logger log = LoggerFactory.getLogger(ContainerEventListener.class);

    private final DockerService dockerService;
    private final WebSocketHandler wsHandler;

    @Autowired
    public ContainerEventListener(DockerService dockerService, WebSocketHandler wsHandler) {
        this.dockerService = dockerService;
        this.wsHandler = wsHandler;
    }

    @EventListener
    @Async
    public void handleContainerOutput(ContainerOutputEvent event) {
        try {
            log.info("Sending output to WebSocket for session {}: '{}'", event.getSessionId(), event.getOutput().replaceAll("\\s+", " "));
            wsHandler.sendMessageToSession(event.getSessionId(), event.getOutput());
        } catch (IOException e) {
            log.error("Error sending output to WebSocket for session {}", event.getSessionId(), e);
        }
    }

    @EventListener
    public void handleContainerInput(ContainerInputEvent event) {
        log.info("Handling container input for session {}: type={}, input='{}'",
                event.getSessionId(), event.getInputType(), event.getInput());

        switch (event.getInputType()) {
            case DATA:
                dockerService.sendInputToContainer(event.getSessionId(), event.getInput());
                break;
            case CLOSE_INPUT:
                dockerService.closeInputForSession(event.getSessionId());
                break;
        }
    }
}