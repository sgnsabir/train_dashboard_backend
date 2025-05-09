package com.banenor.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Streams all published payloads to connected WebSocket clients.
 * Sends heartbeats implicitly by keeping the Flux alive;
 * ignores any inbound messages.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamHandler implements WebSocketHandler {

    private final WebSocketBroadcaster broadcaster;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        log.info("WebSocket session connected: id={}", session.getId());

        // Prepare outbound stream of text messages
        Flux<org.springframework.web.reactive.socket.WebSocketMessage> outbound = broadcaster.flux()
                .map(session::textMessage)
                .doOnError(err -> log.error("Error emitting message to session {}: {}", session.getId(), err.getMessage(), err))
                .doOnCancel(() -> log.debug("Outbound stream cancelled for session {}", session.getId()));

        // Send outbound and concurrently drain inbound (to keep connection alive)
        Mono<Void> send = session.send(outbound)
                .doOnError(err -> log.error("Error sending to session {}: {}", session.getId(), err.getMessage(), err))
                .doFinally(sig -> log.debug("Send stream finalized ({}): session {}", sig, session.getId()));

        Mono<Void> receive = session.receive()
                .doOnNext(msg -> log.debug("Received and ignored message from session {}: {}", session.getId(), msg.getPayloadAsText()))
                .then()
                .doFinally(sig -> log.debug("Receive stream finalized ({}): session {}", sig, session.getId()));

        // Combine send & receive, log on termination
        return Mono.zip(send, receive)
                .then()
                .doFinally(sig -> log.info("WebSocket session closed: id={}, signal={}", session.getId(), sig));
    }
}
