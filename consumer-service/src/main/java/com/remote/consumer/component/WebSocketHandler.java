package com.remote.consumer.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.remote.consumer.service.ContainerInputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler implements org.springframework.web.socket.WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionIdMapping = new ConcurrentHashMap<>(); // custom -> websocket
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ContainerInputService containerInputService;

    @Autowired
    public WebSocketHandler(ContainerInputService containerInputService) {
        this.containerInputService = containerInputService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = getSessionId(session);
        sessions.put(sessionId, session);

        // If we have a custom session ID, map it to the WebSocket session ID
        if (!sessionId.equals(session.getId())) {
            sessionIdMapping.put(sessionId, session.getId());
        }

        log.info("WebSocket connection established for session: {} (WebSocket ID: {})",
                sessionId, session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = getSessionId(session);

        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();
            log.info("Received message from session {}: {}", sessionId, payload);

            try {
                // Parse message to determine if it's input for container
                MessageWrapper messageWrapper = objectMapper.readValue(payload, MessageWrapper.class);

                if ("input".equals(messageWrapper.getType())) {
                    // Send input to the running container via event
                    containerInputService.sendInputToContainer(sessionId, messageWrapper.getData());
                } else if ("close_input".equals(messageWrapper.getType())) {
                    // Close input stream (EOF) via event
                    containerInputService.closeInputForContainer(sessionId);
                }

            } catch (Exception e) {
                // If not JSON, treat as direct input
                containerInputService.sendInputToContainer(sessionId, payload);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = getSessionId(session);
        log.error("Transport error for session {}", sessionId, exception);
        // Clean up container input if there's an error
        containerInputService.closeInputForContainer(sessionId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = getSessionId(session);
        sessions.remove(sessionId);
        sessionIdMapping.remove(sessionId);

        // Clean up container input when connection closes
        containerInputService.closeInputForContainer(sessionId);
        log.info("WebSocket connection closed for session: {} (WebSocket ID: {})",
                sessionId, session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public void sendMessageToSession(String sessionId, String message) throws IOException {
        WebSocketSession session = sessions.get(sessionId);

        // If not found directly, try to find by mapped session ID
        if (session == null) {
            String webSocketId = sessionIdMapping.get(sessionId);
            if (webSocketId != null) {
                session = sessions.get(webSocketId);
            }
        }

        if (session != null && session.isOpen()) {
            synchronized (session) {
                log.info("Sending WebSocket message to session {}: '{}'", sessionId, message.replaceAll("\\s+", " "));
                session.sendMessage(new TextMessage(message));
            }
        } else {
            log.warn("Cannot send message - session {} is null or closed. Available sessions: {}",
                    sessionId, sessions.keySet());
        }
    }

    private String getSessionId(WebSocketSession session) {
        // First try to get custom session ID from URI or attributes
        String customSessionId = null;

        // Try to extract from URI parameters (e.g., /ws?sessionId=xxx)
        if (session.getUri() != null && session.getUri().getQuery() != null) {
            String query = session.getUri().getQuery();
            if (query.contains("sessionId=")) {
                customSessionId = query.substring(query.indexOf("sessionId=") + 10);
                if (customSessionId.contains("&")) {
                    customSessionId = customSessionId.substring(0, customSessionId.indexOf("&"));
                }
            }
        }

        // Try to extract from session attributes
        if (customSessionId == null) {
            Object sessionIdAttr = session.getAttributes().get("sessionId");
            if (sessionIdAttr != null) {
                customSessionId = sessionIdAttr.toString();
            }
        }

        // Fallback to WebSocket session ID
        String finalSessionId = customSessionId != null ? customSessionId : session.getId();
        log.info("Using session ID: {} (WebSocket ID: {}, Custom ID: {})",
                finalSessionId, session.getId(), customSessionId);

        return finalSessionId;
    }

    // Helper class for parsing structured messages
    public static class MessageWrapper {
        private String type;
        private String data;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}