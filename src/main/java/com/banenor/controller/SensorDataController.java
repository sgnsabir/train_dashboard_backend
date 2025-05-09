package com.banenor.controller;

import com.banenor.dto.AggregatedMetricsResponse;
import com.banenor.dto.SensorDataDTO;
import com.banenor.service.CacheService;
import com.banenor.service.SensorDataAggregationService;
import com.banenor.service.SensorDataService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
     * @param startDate The start date for the aggregation.
     * @param endDate   The end date for the aggregation.
     * @return Aggregated metrics response.
     */
    @GetMapping("/aggregated-metrics")
    public Mono<ResponseEntity<AggregatedMetricsResponse>> getAggregatedMetrics(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {

        log.debug("Fetching aggregated metrics between {} and {}", startDate, endDate);

        return sensorDataAggregationService.aggregateSensorDataByRange(startDate, endDate)
                .flatMap(aggregatedData -> {
                    if (aggregatedData != null) {
                        return Mono.just(new AggregatedMetricsResponse(aggregatedData)); // Pass the aggregated data
                    } else {
                        return Mono.just(new AggregatedMetricsResponse("No valid data found.")); // Fallback message if data is invalid
                    }
                })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(404).body(new AggregatedMetricsResponse("No data found within the given range.")))
                .doOnError(error -> log.error("Error while fetching aggregated metrics: {}", error.getMessage(), error))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Endpoint to stream real-time sensor data using Server-Sent Events (SSE).
     *
     * @return Flux of ServerSentEvent containing real-time sensor data.
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
                    log.error("Error in streaming sensor data: {}", error.getMessage(), error);
                })
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(signal -> log.warn("Retrying streaming sensor data due to error: {}", signal.failure().getMessage()))
                );
    }

    /**
     * Helper method to convert a Kafka ReceiverRecord to a ServerSentEvent.
     *
     * @param record Kafka record containing sensor data.
     * @return ServerSentEvent containing the sensor data.
     */
    private ServerSentEvent<SensorDataDTO> toServerSentEvent(ReceiverRecord<String, SensorDataDTO> record) {
        ReceiverOffset offset = record.receiverOffset();
        SensorDataDTO data = record.value();

        // Acknowledge Kafka offset after sending data
        offset.acknowledge();

        // Increment metric for monitoring
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
     *
     * @return Average sensor data response.
     */
    @GetMapping("/averages")
    public Mono<ResponseEntity<AggregatedMetricsResponse>> getSensorAverages() {
        log.debug("Fetching cached sensor averages");

        return cacheService.getCachedAverage("avgSpeed")
                .map(average -> new AggregatedMetricsResponse(average))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.ok(new AggregatedMetricsResponse("No cached average available")))
                .doOnError(error -> log.error("Error while fetching sensor averages: {}", error.getMessage(), error))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
