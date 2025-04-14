package com.banenor.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.StringUtils;

import com.banenor.dto.SensorDataDTO;
import com.banenor.events.MaintenanceRiskEvent;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${kafka.bootstrap-servers:kafka:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:banenor-sensor-data-group}")
    private String consumerGroupId;

    @Value("${kafka.sensor.topic:sensor-data-topic}")
    private String sensorTopic;

    @Value("${kafka.consumer.max-poll-records:50}")
    private int maxPollRecords;

    @Value("${kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${kafka.consumer.enable-auto-commit:false}")
    private boolean enableAutoCommit;

    @Value("${kafka.consumer.isolation-level:read_committed}")
    private String isolationLevel;

    @Value("${kafka.producer.acks:all}")
    private String acks;

    @Value("${kafka.producer.retries:3}")
    private int retries;

    @Value("${kafka.producer.batch-size:16384}")
    private int batchSize;

    private final MeterRegistry meterRegistry;

    public KafkaConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        // Register basic Kafka metrics
        meterRegistry.gauge("kafka.config.bootstrap_servers", bootstrapServers, String::length);
        meterRegistry.gauge("kafka.config.consumer_group_id", consumerGroupId, String::length);
        meterRegistry.gauge("kafka.config.sensor_topic", sensorTopic, String::length);
    }

    // Producer for String messages
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

    // Producer for MaintenanceRiskEvent using JSON serialization
    @Bean
    public Map<String, Object> riskEventProducerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        props.put(ProducerConfig.ACKS_CONFIG, acks);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
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

    // Reactive Kafka Sender for MaintenanceRiskEvent
    @Bean
    public KafkaSender<String, MaintenanceRiskEvent> reactiveRiskEventKafkaSender() {
        Map<String, Object> senderProps = new HashMap<>();
        senderProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        senderProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        senderProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        senderProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        senderProps.put(ProducerConfig.ACKS_CONFIG, acks);
        senderProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        senderProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        
        SenderOptions<String, MaintenanceRiskEvent> senderOptions = SenderOptions.create(senderProps);
        return KafkaSender.create(senderOptions);
    }

    // Consumer Configuration for String messages
    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
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

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.setBatchListener(true);
        return factory;
    }

    // Reactive Kafka Receiver for SensorDataDTO
    @Bean
    public KafkaReceiver<String, SensorDataDTO> sensorDataKafkaReceiver() {
        validateConfiguration();

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.banenor.dto");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, isolationLevel);

        // Add metrics
        props.put(ConsumerConfig.METRIC_REPORTER_CLASSES_CONFIG, 
                "org.apache.kafka.common.metrics.MetricsReporter");
        props.put(ConsumerConfig.METRICS_NUM_SAMPLES_CONFIG, "2");
        props.put(ConsumerConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG, "30000");

        ReceiverOptions<String, SensorDataDTO> receiverOptions = ReceiverOptions.<String, SensorDataDTO>create(props)
                .subscription(java.util.Collections.singletonList(sensorTopic));

        // Add error handling and monitoring
        receiverOptions = receiverOptions
                .commitInterval(java.time.Duration.ofSeconds(5))
                .commitBatchSize(100)
                .maxCommitAttempts(3)
                .commitRetryInterval(java.time.Duration.ofSeconds(1));

        return KafkaReceiver.create(receiverOptions);
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(bootstrapServers)) {
            meterRegistry.counter("kafka.config.validation.errors", "type", "bootstrap_servers").increment();
            throw new IllegalStateException("Kafka bootstrap servers must be configured");
        }
        if (!StringUtils.hasText(consumerGroupId)) {
            meterRegistry.counter("kafka.config.validation.errors", "type", "consumer_group_id").increment();
            throw new IllegalStateException("Kafka consumer group ID must be configured");
        }
        if (!StringUtils.hasText(sensorTopic)) {
            meterRegistry.counter("kafka.config.validation.errors", "type", "sensor_topic").increment();
            throw new IllegalStateException("Kafka sensor topic must be configured");
        }
        if (maxPollRecords <= 0) {
            meterRegistry.counter("kafka.config.validation.errors", "type", "max_poll_records").increment();
            throw new IllegalStateException("Kafka max poll records must be positive");
        }
        if (batchSize <= 0) {
            meterRegistry.counter("kafka.config.validation.errors", "type", "batch_size").increment();
            throw new IllegalStateException("Kafka batch size must be positive");
        }
        if (retries < 0) {
            meterRegistry.counter("kafka.config.validation.errors", "type", "retries").increment();
            throw new IllegalStateException("Kafka retries must be non-negative");
        }
    }
}
