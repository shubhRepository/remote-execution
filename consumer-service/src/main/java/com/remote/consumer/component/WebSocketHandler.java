package com.remote.consumer.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final static Logger log = LoggerFactory.getLogger(WebSocketHandler.class);

    // Keep track of active sessions by sessionId
    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String uri = session.getUri().toString();
        String sessionId = uri.substring(uri.lastIndexOf("/") + 1);

        sessionMap.put(sessionId, session);
        log.info("WebSocket connected for sessionId: {}", sessionId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String uri = session.getUri().toString();
        String sessionId = uri.substring(uri.lastIndexOf("/") + 1);

        sessionMap.remove(sessionId);
        log.info("WebSocket closed for sessionId: {}", sessionId);
    }

    public void sendMessageToSession(String sessionId, String message) throws IOException {
        WebSocketSession session = sessionMap.get(sessionId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }
}
