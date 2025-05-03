package com.banenor.controller;

import com.banenor.dto.AggregatedMetricsResponse;
import com.banenor.dto.SensorDataDTO;
import com.banenor.service.CacheService;
import io.micrometer.core.instrument.MeterRegistry;
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

@RestController
@RequestMapping("/api/v1/sensor")
public class SensorDataController {

    private final CacheService cacheService;
    private final KafkaReceiver<String, SensorDataDTO> sensorDataKafkaReceiver;
    private final MeterRegistry meterRegistry;

    public SensorDataController(CacheService cacheService,
                                KafkaReceiver<String, SensorDataDTO> sensorDataKafkaReceiver,
                                MeterRegistry meterRegistry) {
        this.cacheService = cacheService;
        this.sensorDataKafkaReceiver = sensorDataKafkaReceiver;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/averages")
    public Mono<ResponseEntity<AggregatedMetricsResponse>> getSensorAverages() {
        return cacheService.getCachedAverage("avgSpeed")
                // build response DTO with raw Double
                .map(AggregatedMetricsResponse::new)
                .map(ResponseEntity::ok)
                // fallback when cache is empty
                .defaultIfEmpty(ResponseEntity.ok(
                        new AggregatedMetricsResponse("No cached average available")
                ))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping(path = "/stream", produces = "text/event-stream")
    public Flux<ServerSentEvent<SensorDataDTO>> streamSensorData() {
        return sensorDataKafkaReceiver
                .receive()
                .publishOn(Schedulers.boundedElastic())
                .map(this::toServerSentEvent)
                .doOnError(err -> {
                    meterRegistry.counter("sse.sensor.errors", "error", err.getClass().getSimpleName()).increment();
                })
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(sig -> {
                            // log or metrics before retry
                        })
                );
    }

    private ServerSentEvent<SensorDataDTO> toServerSentEvent(ReceiverRecord<String, SensorDataDTO> record) {
        ReceiverOffset offset = record.receiverOffset();
        SensorDataDTO data = record.value();

        // acknowledge offset
        offset.acknowledge();
        // increment metric
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
