package com.remote.consumer.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ContainerInputEvent extends ApplicationEvent {
    private final String sessionId;
    private final String input;
    private final InputType inputType;

    public enum InputType {
        DATA, CLOSE_INPUT
    }

    public ContainerInputEvent(String sessionId, String input, InputType inputType) {
        super(sessionId);
        this.sessionId = sessionId;
        this.input = input;
        this.inputType = inputType;
    }

}