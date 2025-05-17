package com.banenor.controller;

import com.banenor.dto.AggregatedMetricsRequest;
import com.banenor.dto.AggregatedMetricsResponse;
import com.banenor.dto.SensorDataDTO;
import com.banenor.service.CacheService;
import com.banenor.service.SensorDataAggregationService;
import com.banenor.service.SensorDataService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

import jakarta.validation.Valid;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/sensor")
public class SensorDataController {

    private final CacheService cacheService;
    private final SensorDataService sensorDataService;
    private final SensorDataAggregationService sensorDataAggregationService;
    private final KafkaReceiver<String, SensorDataDTO> sensorDataKafkaReceiver;
    private final MeterRegistry meterRegistry;

    public SensorDataController(CacheService cacheService,
                                SensorDataService sensorDataService,
                                SensorDataAggregationService sensorDataAggregationService,
                                KafkaReceiver<String, SensorDataDTO> sensorDataKafkaReceiver,
                                MeterRegistry meterRegistry) {
        this.cacheService = cacheService;
        this.sensorDataService = sensorDataService;
        this.sensorDataAggregationService = sensorDataAggregationService;
        this.sensorDataKafkaReceiver = sensorDataKafkaReceiver;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Endpoint to fetch aggregated sensor metrics within a specified date range.
     *
     * Dates must be in ISO-8601 format (e.g. {@code 2025-05-01T00:00:00}).
     *
     * @param req container for startDate and endDate
     * @return AggregatedMetricsResponse wrapped in 200 or an error status
     */
    @GetMapping("/aggregated-metrics")
    public Mono<ResponseEntity<AggregatedMetricsResponse>> getAggregatedMetrics(
            @Valid @ParameterObject AggregatedMetricsRequest req) {

        // Convert to ISO strings for the existing service API
        String start = req.getStartDate().format(DateTimeFormatter.ISO_DATE_TIME);
        String end   = req.getEndDate().format(DateTimeFormatter.ISO_DATE_TIME);

        log.debug("Fetching aggregated metrics between {} and {}", start, end);

        return sensorDataAggregationService
                .aggregateSensorDataByRange(start, end)
                .flatMap(data -> Mono.just(new AggregatedMetricsResponse(data)))
                .defaultIfEmpty(new AggregatedMetricsResponse("No data found within the given range."))
                .map(ResponseEntity::ok)
                .doOnError(error -> log.error("Error while fetching aggregated metrics", error))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Endpoint to stream real-time sensor data using Server-Sent Events (SSE).
     */
    @GetMapping(path = "/stream", produces = "text/event-stream")
    public Flux<ServerSentEvent<SensorDataDTO>> streamSensorData() {
        log.debug("Starting to stream real-time sensor data via SSE");

        return sensorDataKafkaReceiver
                .receive()
                .publishOn(Schedulers.boundedElastic())
                .map(this::toServerSentEvent)
                .doOnError(error -> {
                    meterRegistry.counter("sse.sensor.errors", "error", error.getClass().getSimpleName()).increment();
                    log.error("Error in streaming sensor data", error);
                })
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(sig ->
                                log.warn("Retrying SSE due to: {}", sig.failure().getMessage()))
                );
    }

    /**
     * Helper to convert a Kafka ReceiverRecord to a ServerSentEvent.
     */
    private ServerSentEvent<SensorDataDTO> toServerSentEvent(ReceiverRecord<String, SensorDataDTO> record) {
        ReceiverOffset offset = record.receiverOffset();
        SensorDataDTO data = record.value();

        // Acknowledge Kafka offset
        offset.acknowledge();

        // Increment metric
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

    /**
     * Endpoint to fetch average sensor data (cached).
     */
    @GetMapping("/averages")
    public Mono<ResponseEntity<AggregatedMetricsResponse>> getSensorAverages() {
        log.debug("Fetching cached sensor averages");

        return cacheService.getCachedAverage("avgSpeed")
                .map(AggregatedMetricsResponse::new)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.ok(new AggregatedMetricsResponse("No cached average available")))
                .doOnError(error -> log.error("Error while fetching sensor averages", error))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
