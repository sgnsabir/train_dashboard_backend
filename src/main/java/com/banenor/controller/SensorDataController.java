package com.banenor.controller;

import com.banenor.dto.AggregatedMetricsResponse;
import com.banenor.dto.SensorDataDTO;
import com.banenor.service.CacheService;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/v1/sensor")
@Tag(name = "Sensor Data", description = "Endpoints for cached metrics and streaming raw sensor data")
@RequiredArgsConstructor
public class SensorDataController {

    private final CacheService cacheService;
    private final KafkaReceiver<String, SensorDataDTO> sensorDataKafkaReceiver;
    private final MeterRegistry meterRegistry;

    @Operation(
            summary = "Get Cached Sensor Averages",
            description = "Fetch aggregated sensor metrics (e.g., average speed) from cache"
    )
    @GetMapping("/averages")
    public Mono<ResponseEntity<AggregatedMetricsResponse>> getSensorAverages() {
        return cacheService.getCachedAverage("avgSpeed")
                .map(avg -> new AggregatedMetricsResponse(Mono.just(avg)))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.ok(new AggregatedMetricsResponse("No cached average available")))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(
            summary = "Stream Raw Sensor Data",
            description = "Stream raw sensor data from Kafka via Server‑Sent Events (SSE)"
    )
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<SensorDataDTO>> streamSensorData() {
        return sensorDataKafkaReceiver
                .receive()  // Flux<ReceiverRecord<String, SensorDataDTO>>
                .publishOn(Schedulers.boundedElastic())
                .map(this::toServerSentEvent)
                .doOnError(err -> {
                    meterRegistry.counter("sse.sensor.errors", "error", err.getClass().getSimpleName()).increment();
                    log.error("Error in SSE stream for sensor data", err);
                })
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(sig -> log.warn("Retrying SSE after error: {}", sig.failure().getMessage())));
    }

    private ServerSentEvent<SensorDataDTO> toServerSentEvent(ReceiverRecord<String, SensorDataDTO> record) {
        ReceiverOffset offset = record.receiverOffset();
        SensorDataDTO data = record.value();

        // Acknowledge offset before sending
        offset.acknowledge();

        // Instrument metrics
        meterRegistry.counter("sse.sensor.sent",
                        "topic", record.topic(),
                        "partition", String.valueOf(record.partition()))
                .increment();

        return ServerSentEvent.<SensorDataDTO>builder()
                .id(String.valueOf(offset.offset()))
                .event("sensor-data")
                .data(data)
                .build();
    }
}
