package com.banenor.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.StringUtils;

import io.micrometer.core.instrument.MeterRegistry;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import com.banenor.dto.SensorDataDTO;
import com.banenor.events.MaintenanceRiskEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {

    // ──── Bootstrap & Topics ───────────────────────────────────
    @Value("${kafka.bootstrap-servers:kafka:9092}")
    private String bootstrapServers;

    @Value("${kafka.sensor.topic:sensor-data-topic}")
    private String sensorTopic;

    @Value("${app.kafka.maintenance-risk-topic:maintenance-risk-events}")
    private String maintenanceRiskTopic;

    // ──── Consumer Groups ──────────────────────────────────────
    @Value("${spring.kafka.consumer.group-id:banenor-sensor-data-group}")
    private String sensorConsumerGroupId;

    @Value("${spring.kafka.consumer.risk.group-id:maintenance-risk-group}")
    private String riskConsumerGroupId;

    // ──── Consumer Tuning ──────────────────────────────────────
    @Value("${kafka.consumer.max-poll-records:50}")
    private int maxPollRecords;
    @Value("${kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;
    @Value("${kafka.consumer.enable-auto-commit:false}")
    private boolean enableAutoCommit;
    @Value("${kafka.consumer.isolation-level:read_committed}")
    private String isolationLevel;

    // ──── Producer Tuning ──────────────────────────────────────
    @Value("${kafka.producer.acks:all}")
    private String acks;
    @Value("${kafka.producer.retries:3}")
    private int retries;
    @Value("${kafka.producer.batch-size:16384}")
    private int batchSize;

    private final MeterRegistry meterRegistry;

    public KafkaConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        // Expose basic config via metrics
        meterRegistry.gauge("kafka.config.bootstrap_servers", bootstrapServers, String::length);
        meterRegistry.gauge("kafka.config.sensor_topic", sensorTopic, String::length);
        meterRegistry.gauge("kafka.config.risk_topic", maintenanceRiskTopic, String::length);
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(bootstrapServers)) {
            meterRegistry.counter("kafka.config.errors", "param", "bootstrapServers").increment();
            throw new IllegalStateException("Kafka bootstrap servers must be configured");
        }
        if (!StringUtils.hasText(sensorConsumerGroupId) || !StringUtils.hasText(riskConsumerGroupId)) {
            meterRegistry.counter("kafka.config.errors", "param", "consumerGroupId").increment();
            throw new IllegalStateException("Kafka consumer group IDs must be configured");
        }
    }

    // ──────────────────────────────────────────────────────────
    // Imperative String Producer
    // ──────────────────────────────────────────────────────────
    @Bean
    public Map<String, Object> stringProducerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, acks);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        return props;
    }

    @Bean
    public ProducerFactory<String, String> stringProducerFactory() {
        return new DefaultKafkaProducerFactory<>(stringProducerConfigs());
    }

    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate() {
        return new KafkaTemplate<>(stringProducerFactory());
    }

    // ──────────────────────────────────────────────────────────
    // Imperative MaintenanceRiskEvent Producer
    // ──────────────────────────────────────────────────────────
    @Bean
    public Map<String, Object> riskEventProducerConfigs() {
        Map<String, Object> props = new HashMap<>(stringProducerConfigs());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return props;
    }

    @Bean
    public ProducerFactory<String, MaintenanceRiskEvent> riskEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(riskEventProducerConfigs());
    }

    @Bean
    public KafkaTemplate<String, MaintenanceRiskEvent> riskEventKafkaTemplate() {
        return new KafkaTemplate<>(riskEventProducerFactory());
    }

    // ──────────────────────────────────────────────────────────
    // Reactive MaintenanceRiskEvent Producer (Reactor Kafka)
    // ──────────────────────────────────────────────────────────
    @Bean
    public KafkaSender<String, MaintenanceRiskEvent> reactiveRiskEventKafkaSender() {
        SenderOptions<String, MaintenanceRiskEvent> senderOptions =
                SenderOptions.create(riskEventProducerConfigs());
        return KafkaSender.create(senderOptions);
    }

    // ──────────────────────────────────────────────────────────
    // Imperative String Consumer (for SensorData, etc.)
    // ──────────────────────────────────────────────────────────
    @Bean
    public Map<String, Object> consumerConfigs() {
        validateConfiguration();
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, sensorConsumerGroupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, isolationLevel);
        return props;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean(name = "stringKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String>
    kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.setBatchListener(true);
        return factory;
    }

    // ──────────────────────────────────────────────────────────
    // Imperative JSON Consumer for MaintenanceRiskEvent
    // ──────────────────────────────────────────────────────────
    @Bean
    public Map<String, Object> riskConsumerConfigs() {
        // start with the base sensor consumer props, then override:
        Map<String, Object> props = new HashMap<>(consumerConfigs());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, riskConsumerGroupId);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MaintenanceRiskEvent.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.banenor.events");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return props;
    }

    @Bean
    public ConsumerFactory<String, MaintenanceRiskEvent> riskConsumerFactory() {
        JsonDeserializer<MaintenanceRiskEvent> deserializer =
                new JsonDeserializer<>(MaintenanceRiskEvent.class, false)
                        .trustedPackages("com.banenor.events")
                        .ignoreTypeHeaders();

        return new DefaultKafkaConsumerFactory<>(
                riskConsumerConfigs(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean(name = "riskKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, MaintenanceRiskEvent>
    riskKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, MaintenanceRiskEvent>();
        factory.setConsumerFactory(riskConsumerFactory());
        factory.setConcurrency(3);
        factory.setBatchListener(false);
        return factory;
    }

    // ──────────────────────────────────────────────────────────
    // Reactive JSON Receiver for SensorDataDTO (Reactor Kafka)
    // ──────────────────────────────────────────────────────────
    @Bean
    public KafkaReceiver<String, SensorDataDTO> sensorDataKafkaReceiver() {
        validateConfiguration();

        Map<String, Object> props = new HashMap<>(consumerConfigs());
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SensorDataDTO.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.banenor.dto");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        JsonDeserializer<SensorDataDTO> sensorDeserializer =
                new JsonDeserializer<>(SensorDataDTO.class, false)
                        .trustedPackages("com.banenor.dto");

        ReceiverOptions<String, SensorDataDTO> receiverOptions =
                ReceiverOptions.<String, SensorDataDTO>create(props)
                        .withKeyDeserializer(new StringDeserializer())
                        .withValueDeserializer(sensorDeserializer)
                        .subscription(Collections.singletonList(sensorTopic));

        return KafkaReceiver.create(receiverOptions);
    }

    // ──────────────────────────────────────────────────────────
    // Topic Auto-Creation for MaintenanceRiskEvent
    // ──────────────────────────────────────────────────────────
    @Bean
    public NewTopic maintenanceRiskTopic() {
        log.info("Ensuring topic '{}' exists (partitions=3, replicas=1)", maintenanceRiskTopic);
        return new NewTopic(maintenanceRiskTopic, 3, (short)1);
    }
}
