package com.remote.consumer.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ContainerOutputEvent extends ApplicationEvent {
    private final String sessionId;
    private final String output;

    public ContainerOutputEvent(String sessionId, String output) {
        super(sessionId);
        this.sessionId = sessionId;
        this.output = output;
    }

}