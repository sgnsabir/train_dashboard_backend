package com.banenor.kafka;

import com.banenor.dto.SensorDataDTO;
import com.banenor.service.DataService;
import com.banenor.service.RealtimeAlertService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
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
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorDataConsumer {

    private final DataService dataService;
    private final RealtimeAlertService alertService;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    @Value("${kafka.sensor.topic}")
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
                .description("Time taken to process sensor data")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    @Bean
    public KafkaReceiver<String, SensorDataDTO> kafkaReceiver() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, consumerGroupId + "-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "com.banenor.dto.SensorDataDTODeserializer");
        props.put("client.telemetry.reporter", Collections.singletonList(new NoOpMetricsReporter()));

        ReceiverOptions<String, SensorDataDTO> receiverOptions =
                ReceiverOptions.<String, SensorDataDTO>create(props)
                        .subscription(Collections.singleton(sensorTopic));

        return KafkaReceiver.create(receiverOptions);
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
            kafkaReceiver().receive()
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
                            }))
                    .subscribe();

            kafkaReceiver().receiveAtmostOnce()
                    .doOnNext(record -> meterRegistry.counter("kafka.partition.assignments",
                            "topic", record.topic(),
                            "partition", String.valueOf(record.partition())).increment())
                    .subscribe();
            isRunning.set(true);
        }
    }

    @Bean
    @ConditionalOnProperty(value = "kafka.consumer.health-check-enabled", havingValue = "true", matchIfMissing = true)
    public HealthIndicator kafkaConsumerHealthIndicator() {
        return () -> {
            if (!healthCheckEnabled) return org.springframework.boot.actuate.health.Health.up().build();
            if (!isRunning.get()) return org.springframework.boot.actuate.health.Health.down().withDetail("reason", "Consumer not running").build();
            if (!healthy.get()) return org.springframework.boot.actuate.health.Health.down().withDetail("reason", "Consumer unhealthy")
                    .withDetail("lastError", lastError != null ? lastError.getMessage() : "Unknown").build();
            return org.springframework.boot.actuate.health.Health.up().withDetail("topic", sensorTopic).withDetail("groupId", consumerGroupId).build();
        };
    }

    private Mono<Void> processRecord(ReceiverRecord<String, SensorDataDTO> record) {
        return processingTimer.record(() -> {
            SensorDataDTO sensorData = record.value();
            if (sensorData == null) return Mono.empty();
            meterRegistry.counter("sensor.data.received",
                    "sensor_id", sensorData.getSensorId(),
                    "train_no", String.valueOf(sensorData.getTrainNo())).increment();
            return Mono.defer(() -> {
                        // Serialize sensorData to JSON string because processSensorData expects a String.
                        String sensorDataJson;
                        try {
                            sensorDataJson = objectMapper.writeValueAsString(sensorData);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to serialize sensor data: {}", e.getMessage(), e);
                            return Mono.empty();
                        }
                        Mono<Void> maintenance = dataService.processSensorData(sensorDataJson)
                                .onErrorResume(e -> Mono.empty());
                        Mono<Void> alert = alertService.monitorAndAlert(sensorData.getTrainNo(), defaultAlertEmail)
                                .onErrorResume(e -> Mono.empty());
                        return Mono.when(maintenance, alert);
                    })
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofMillis(retryDelayMs)))
                    .doOnSuccess(v -> acknowledgeRecord(record))
                    .doOnError(e -> handleProcessingError(record, e));
        });
    }

    private void acknowledgeRecord(ReceiverRecord<String, SensorDataDTO> record) {
        ReceiverOffset offset = record.receiverOffset();
        offset.acknowledge();
        meterRegistry.counter("sensor.data.processed",
                "partition", String.valueOf(record.partition()),
                "topic", record.topic()).increment();
    }

    private void handleProcessingError(ReceiverRecord<String, SensorDataDTO> record, Throwable error) {
        meterRegistry.counter("sensor.data.processing.failures",
                "topic", record.topic(),
                "error", error.getClass().getSimpleName()).increment();
    }

    private void handleConsumerError(Throwable error) {
        meterRegistry.counter("sensor.data.consumer.errors",
                "error", error.getClass().getSimpleName()).increment();
    }

    private static class NoOpMetricsReporter implements MetricsReporter {
        @Override public void init(java.util.List<KafkaMetric> metrics) {}
        @Override public void metricChange(KafkaMetric metric) {}
        @Override public void metricRemoval(KafkaMetric metric) {}
        @Override public void close() {}
        @Override public void configure(Map<String, ?> configs) {}
    }
}
