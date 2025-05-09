package com.banenor.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

/**
 * Registers the WebSocket endpoint and handler adapter.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebSocketConfig {

    private static final String WS_ENDPOINT = "/ws/stream";

    /**
     * Map the WebSocket endpoint to our StreamHandler.
     */
    @Bean
    public HandlerMapping webSocketMapping(StreamHandler streamHandler) {
        log.info("Registering WebSocket endpoint {}", WS_ENDPOINT);
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(Map.of(WS_ENDPOINT, (WebSocketHandler) streamHandler));
        mapping.setOrder(-1);
        return mapping;
    }

    /**
     * Adapter needed to process WebSocket handshakes.
     */
    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
