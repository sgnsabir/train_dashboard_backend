package com.banenor.kafka;

import com.banenor.dto.SensorDataDTO;
import com.banenor.service.DataService;
import com.banenor.service.RealtimeAlertService;
import com.banenor.websocket.WebSocketBroadcaster;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Consumes sensor data from Kafka, processes it, triggers real-time alerts,
 * and broadcasts sensor updates over WebSocket for frontend dashboards.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensorDataConsumer {

    private final KafkaReceiver<String, SensorDataDTO> kafkaReceiver;
    private final DataService dataService;
    private final RealtimeAlertService alertService;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final WebSocketBroadcaster broadcaster;

    @Value("${kafka.sensor.topic:sensor-data-topic}")
    private String sensorTopic;
    @Value("${spring.kafka.consumer.group-id:banenor-sensor-data-group}")
    private String consumerGroupId;
    @Value("${kafka.consumer.max-retry-attempts:3}")
    private int maxRetryAttempts;
    @Value("${kafka.consumer.retry-delay-ms:1000}")
    private long retryDelayMs;
    @Value("${kafka.consumer.batch-size:100}")
    private int batchSize;
    @Value("${alert.default-email}")
    private String defaultAlertEmail;
    @Value("${kafka.consumer.shutdown-timeout-seconds:30}")
    private int shutdownTimeoutSeconds;
    @Value("${kafka.consumer.health-check-enabled:true}")
    private boolean healthCheckEnabled;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean healthy = new AtomicBoolean(true);
    private volatile Throwable lastError;
    private Timer processingTimer;

    @PostConstruct
    public void init() {
        this.processingTimer = Timer.builder("sensor.data.processing")
                .description("Latency of sensor data processing pipeline")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    @PreDestroy
    public void shutdown() {
        if (isRunning.compareAndSet(true, false)) {
            try {
                Thread.sleep(Duration.ofSeconds(shutdownTimeoutSeconds).toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @EventListener(ApplicationStartedEvent.class)
    public void startConsumer() {
        if (isRunning.compareAndSet(false, true)) {
            kafkaReceiver.receive()
                    .publishOn(Schedulers.boundedElastic())
                    .bufferTimeout(batchSize, Duration.ofSeconds(5))
                    .flatMap(records -> Flux.fromIterable(records).flatMap(this::processRecord))
                    .doOnError(this::handleConsumerError)
                    .doOnNext(v -> healthy.set(true))
                    .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofMinutes(5))
                            .doBeforeRetry(signal -> {
                                healthy.set(false);
                                lastError = signal.failure();
                                log.warn("Kafka consumer retry #{} due to {}", signal.totalRetries(), signal.failure().getMessage());
                            }))
                    .subscribe();

            log.info("Kafka consumer started for topic='{}', group='{}'", sensorTopic, consumerGroupId);
        }
    }

    @Bean
    @ConditionalOnProperty(value = "kafka.consumer.health-check-enabled", havingValue = "true", matchIfMissing = true)
    public HealthIndicator kafkaConsumerHealthIndicator() {
        return () -> {
            if (!healthCheckEnabled) {
                return org.springframework.boot.actuate.health.Health.unknown().build();
            }
            if (!isRunning.get()) {
                return org.springframework.boot.actuate.health.Health.down().withDetail("reason", "Consumer not running").build();
            }
            if (!healthy.get()) {
                return org.springframework.boot.actuate.health.Health.down()
                        .withDetail("reason", "Unhealthy consumer")
                        .withDetail("lastError", lastError != null ? lastError.getMessage() : "none")
                        .build();
            }
            return org.springframework.boot.actuate.health.Health.up()
                    .withDetail("topic", sensorTopic)
                    .withDetail("groupId", consumerGroupId)
                    .build();
        };
    }

    private Mono<Void> processRecord(ReceiverRecord<String, SensorDataDTO> record) {
        SensorDataDTO sensorData = record.value();
        if (sensorData == null) {
            return Mono.empty();
        }

        meterRegistry.counter("sensor.data.received",
                        "sensor_id", sensorData.getSensorId(),
                        "train_no", String.valueOf(sensorData.getTrainNo()))
                .increment();

        long start = System.nanoTime();

        Mono<Void> pipeline = Mono.defer(() -> {
                    String payload;
                    try {
                        payload = objectMapper.writeValueAsString(sensorData);
                    } catch (JsonProcessingException e) {
                        log.error("Serialization error for sensorData={} : {}", sensorData, e.getMessage(), e);
                        return Mono.empty();
                    }

                    // 1) Persist data
                    // 2) Broadcast to WebSocket clients
                    // 3) Trigger alerts
                    return dataService.processSensorData(payload)
                            .then(Mono.fromRunnable(() -> {
                                broadcaster.publish(sensorData, "SENSOR_DATA");
                                log.debug("Broadcasted SENSOR_DATA for trainNo={}", sensorData.getTrainNo());
                            }))
                            .then(alertService.monitorAndAlert(sensorData.getTrainNo(), defaultAlertEmail)
                                    .onErrorResume(ex -> {
                                        log.warn("Alert dispatch failed for trainNo={} : {}", sensorData.getTrainNo(), ex.getMessage());
                                        return Mono.empty();
                                    }));
                })
                .timeout(Duration.ofSeconds(30))
                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofMillis(retryDelayMs))
                        .doBeforeRetry(sig -> log.warn("Processing retry #{} for record offset={} : {}",
                                sig.totalRetries(), record.offset(), sig.failure().getMessage())))
                .doOnSuccess(v -> acknowledge(record))
                .doOnError(e -> handlingProcessingError(record, e));

        return pipeline.doFinally(sig -> {
            long duration = System.nanoTime() - start;
            processingTimer.record(duration, TimeUnit.NANOSECONDS);
        });
    }

    private void acknowledge(ReceiverRecord<String, SensorDataDTO> record) {
        ReceiverOffset offset = record.receiverOffset();
        offset.acknowledge();
        meterRegistry.counter("sensor.data.processed",
                        "partition", String.valueOf(record.partition()),
                        "topic", record.topic())
                .increment();
    }

    private void handlingProcessingError(ReceiverRecord<String, SensorDataDTO> record, Throwable error) {
        meterRegistry.counter("sensor.data.processing.failures",
                        "topic", record.topic(),
                        "partition", String.valueOf(record.partition()),
                        "error", error.getClass().getSimpleName())
                .increment();
        log.error("Error processing record topic={} partition={} offset={} : {}",
                record.topic(), record.partition(), record.offset(), error.getMessage(), error);
    }

    private void handleConsumerError(Throwable error) {
        healthy.set(false);
        meterRegistry.counter("sensor.data.consumer.errors",
                        "error", error.getClass().getSimpleName())
                .increment();
        log.error("Kafka consumer fatal error: {}", error.getMessage(), error);
    }
}
