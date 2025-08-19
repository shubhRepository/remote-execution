package com.remote.consumer.config;

import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import java.util.Map;

public class SessionIdInterceptor extends HttpSessionHandshakeInterceptor {
    @Override
    public boolean beforeHandshake(
            org.springframework.http.server.ServerHttpRequest request,
            org.springframework.http.server.ServerHttpResponse response,
            org.springframework.web.socket.WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        String uri = request.getURI().toString();
        String sessionId = uri.substring(uri.lastIndexOf("/") + 1);
        attributes.put("sessionId", sessionId);
        return true;
    }
}