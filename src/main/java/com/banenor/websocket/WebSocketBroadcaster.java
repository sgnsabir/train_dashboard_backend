package com.banenor.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;

/**
 * Broadcasts events to all connected WebSocket clients.
 */
@Slf4j
@Component
public class WebSocketBroadcaster {

    // Jackson mapper for serializing payloads
    private final ObjectMapper mapper = new ObjectMapper();

    // Reactor sink for multicasting messages
    private final Sinks.Many<String> sink = Sinks.many().multicast().directBestEffort();

    /**
     * Provides a Flux of JSON-encoded messages for WebSocketHandler to subscribe to.
     */
    public Flux<String> flux() {
        return sink.asFlux()
                .doOnSubscribe(sub -> log.debug("New WebSocket subscriber"))
                .doOnNext(msg -> log.trace("Emitting WebSocket message: {}", msg))
                .doOnError(err -> log.error("Error in WebSocket flux", err));
    }

    /**
     * Publishes a payload with a given type to all subscribers.
     *
     * @param payload the data object to send
     * @param type    a message-type identifier (e.g. "ALERT", "SENSOR_DATA")
     */
    public void publish(Object payload, String type) {
        try {
            String json = mapper.writeValueAsString(Map.of(
                    "type", type,
                    "data", payload
            ));
            // Emit, failing fast on error
            sink.emitNext(json, Sinks.EmitFailureHandler.FAIL_FAST);
            log.debug("Published WebSocket message of type {}", type);
        } catch (JsonProcessingException e) {
            log.error("Serialization error for WebSocket payload (type={}): {}", type, e.getMessage(), e);
        }
    }
}
