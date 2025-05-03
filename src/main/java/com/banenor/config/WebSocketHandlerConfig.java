package com.banenor.config;

import org.springframework.lang.NonNull;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

import reactor.core.publisher.Mono;

public class WebSocketHandlerConfig implements WebSocketHandler {

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull WebSocketSession session) {
        // For example, echo back every message received.
        return session.send(
                session.receive()
                        .map(msg -> session.textMessage("Echo: " + msg.getPayloadAsText()))
        );
    }
}
