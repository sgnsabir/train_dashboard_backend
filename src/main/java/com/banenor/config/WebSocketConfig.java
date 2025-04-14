package com.banenor.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Configuration
public class WebSocketConfig {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(30);
    private static final Duration CONNECTION_TIMEOUT = Duration.ofMinutes(5);

    @Value("#{'${websocket.allowedOrigins:http://localhost:3000}'.split(',')}")
    private final List<String> allowedOrigins = new ArrayList<>();

    @Value("${websocket.max-frame-size:65536}")
    private Integer maxFrameSize;

    @Value("${websocket.max-connections:1000}")
    private Integer maxConnections;

    public WebSocketConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        // Register metrics
        meterRegistry.gauge("websocket.connections.active", activeConnections);
    }

    public static class SecureEchoWebSocketHandler implements WebSocketHandler {
        private final MeterRegistry meterRegistry;
        private final AtomicInteger activeConnections;
        private final Map<String, WebSocketSession> sessions;
        private final Integer maxFrameSize;

        public SecureEchoWebSocketHandler(MeterRegistry meterRegistry, AtomicInteger activeConnections, 
                                        Map<String, WebSocketSession> sessions, Integer maxFrameSize) {
            this.meterRegistry = meterRegistry;
            this.activeConnections = activeConnections;
            this.sessions = sessions;
            this.maxFrameSize = maxFrameSize;
        }

        @Override
        @NonNull
        public Mono<Void> handle(@NonNull WebSocketSession session) {
            String sessionId = session.getId();
            return Mono.defer(() -> {
                if (activeConnections.get() >= 1000) {
                    return session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Max connections reached"));
                }

                // Register connection metrics
                activeConnections.incrementAndGet();
                sessions.put(sessionId, session);
                meterRegistry.counter("websocket.connections.total").increment();

                // Setup heartbeat
                Flux<WebSocketMessage> heartbeat = Flux.interval(HEARTBEAT_INTERVAL)
                    .map(i -> session.textMessage("heartbeat"))
                    .doOnError(e -> handleError(session, e));

                // Handle incoming messages
                Flux<WebSocketMessage> echo = session.receive()
                    .timeout(CONNECTION_TIMEOUT)
                    .map(msg -> {
                        meterRegistry.counter("websocket.messages.received").increment();
                        return validateAndProcessMessage(msg);
                    })
                    .map(msg -> session.textMessage("Echo: " + msg))
                    .doOnError(e -> handleError(session, e));

                // Merge heartbeat and echo streams
                return session.send(Flux.merge(heartbeat, echo))
                    .doFinally(signalType -> {
                        activeConnections.decrementAndGet();
                        sessions.remove(sessionId);
                        meterRegistry.counter("websocket.connections.closed").increment();
                    })
                    .subscribeOn(Schedulers.boundedElastic());
            });
        }

        private String validateAndProcessMessage(WebSocketMessage message) {
            String payload = message.getPayloadAsText();
            if (payload.length() > maxFrameSize) { // Use configured max frame size
                throw new IllegalArgumentException("Message too large");
            }
            // Add any necessary message validation/sanitization
            return payload;
        }

        private void handleError(WebSocketSession session, Throwable error) {
            meterRegistry.counter("websocket.errors", "type", error.getClass().getSimpleName()).increment();
            session.close(CloseStatus.SERVER_ERROR.withReason(error.getMessage()))
                .subscribe();
        }
    }

    public class SecureOriginCheckingWebSocketHandler implements WebSocketHandler {
        private final WebSocketHandler delegate;
        private final List<String> allowedOrigins;
        private final Integer maxConnections;

        public SecureOriginCheckingWebSocketHandler(WebSocketHandler delegate, List<String> allowedOrigins, 
                                                  Integer maxConnections) {
            this.delegate = delegate;
            this.allowedOrigins = allowedOrigins;
            this.maxConnections = maxConnections;
        }

        @Override
        @NonNull
        public Mono<Void> handle(@NonNull WebSocketSession session) {
            return Mono.defer(() -> {
                // Check connection limit
                if (activeConnections.get() >= maxConnections) {
                    return session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Max connections reached"));
                }

                // Validate origin
                List<String> origins = session.getHandshakeInfo().getHeaders().get("Origin");
                if (origins == null || origins.isEmpty() || !allowedOrigins.contains(origins.get(0))) {
                    meterRegistry.counter("websocket.rejected.origin").increment();
                    return session.close(CloseStatus.POLICY_VIOLATION.withReason("Origin not allowed"));
                }

                // Rate limiting and additional security checks can be added here

                return delegate.handle(session)
                    .doOnError(e -> meterRegistry.counter("websocket.errors").increment());
            });
        }
    }

    @Bean
    public WebSocketHandler myWebSocketHandler() {
        return new SecureOriginCheckingWebSocketHandler(
            new SecureEchoWebSocketHandler(meterRegistry, activeConnections, sessions, maxFrameSize),
            allowedOrigins,
            maxConnections
        );
    }

    @Bean
    public SimpleUrlHandlerMapping webSocketHandlerMapping(WebSocketHandler myWebSocketHandler) {
        Map<String, WebSocketHandler> urlMap = Map.of("/ws", myWebSocketHandler);
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(urlMap);
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
