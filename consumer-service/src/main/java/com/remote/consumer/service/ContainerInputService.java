package com.remote.consumer.service;

import com.remote.consumer.event.ContainerInputEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class ContainerInputService {

    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ContainerInputService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void sendInputToContainer(String sessionId, String input) {
        eventPublisher.publishEvent(
                new ContainerInputEvent(sessionId, input, ContainerInputEvent.InputType.DATA)
        );
    }

    public void closeInputForContainer(String sessionId) {
        eventPublisher.publishEvent(
                new ContainerInputEvent(sessionId, null, ContainerInputEvent.InputType.CLOSE_INPUT)
        );
    }
}