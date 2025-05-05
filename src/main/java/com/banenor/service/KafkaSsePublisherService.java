package com.banenor.service;

import com.banenor.dto.AxlesDataDTO;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Publishes incoming AxlesDataDTO Kafka events as an SSE stream.
 * Uses a multicast sink so multiple subscribers receive real-time updates,
 * filtered by train number and measurement point (TP).
 */
@Slf4j
@Service
public class KafkaSsePublisherService implements DisposableBean {

    /**
     * Multicast sink with unbounded buffering.
     * You may switch to a bounded buffer (e.g. onBackpressureBuffer(1024))
     * if you need to cap memory usage.
     */
    private final Sinks.Many<AxlesDataDTO> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();

    public KafkaSsePublisherService() {
        log.info("Initialized KafkaSsePublisherService");
    }

    /**
     * Kafka listener that receives AxlesDataDTO events
     * and emits them into the sink.
     */
    @KafkaListener(
            topics = "${kafka.axles.topic:axles-data}",
            groupId = "${kafka.axles.group:axles-sse-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAxlesDataEvent(AxlesDataDTO event) {
        log.debug("Received AxlesDataDTO from Kafka: {}", event);
        Sinks.EmitResult result = sink.tryEmitNext(event);
        if (result.isFailure()) {
            log.error("Failed to emit AxlesDataDTO to sink, result={}", result);
        }
    }

    /**
     * Returns a Flux that streams only those AxlesDataDTO events
     * matching the given train number and TP.
     *
     * @param trainNo the train number to filter by
     * @param tp      the measurement point (e.g. "TP1") to filter by
     */
    public Flux<AxlesDataDTO> stream(int trainNo, String tp) {
        log.debug("New SSE subscription: trainNo={}, tp={}", trainNo, tp);
        return sink.asFlux()
                .filter(dto ->
                        dto.getTrainNo() != null && dto.getTrainNo().intValue() == trainNo)
                .filter(dto ->
                        dto.getMeasurementPoint() != null
                                && dto.getMeasurementPoint().equalsIgnoreCase(tp))
                .doOnCancel(() ->
                        log.info("SSE subscription cancelled: trainNo={}, tp={}", trainNo, tp))
                .doOnError(err ->
                        log.error("Error in SSE stream for trainNo={}, tp={}: {}",
                                trainNo, tp, err.getMessage(), err));
    }

    /**
     * Completes the sink on shutdown.
     */
    @PreDestroy
    @Override
    public void destroy() {
        log.info("Shutting down KafkaSsePublisherService, completing sink");
        sink.tryEmitComplete();
    }
}
