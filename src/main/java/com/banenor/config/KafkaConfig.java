package com.banenor.config;

import com.banenor.dto.SensorMeasurementDTO;
import com.banenor.events.MaintenanceRiskEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfig {

    private final MeterRegistry    meterRegistry;
    private final KafkaProperties  kafkaProperties;

    @Value("${kafka.bootstrap-servers:kafka:9092}")
    private String bootstrapServers;

    @Value("${kafka.sensor.topic:sensor-data-topic}")
    private String sensorTopic;

    @Value("${app.kafka.maintenance-risk-topic:maintenance-risk-events}")
    private String maintenanceRiskTopic;

    @Value("${spring.kafka.consumer.group-id:banenor-sensor-data-group}")
    private String sensorConsumerGroupId;

    @Value("${spring.kafka.consumer.risk.group-id:maintenance-risk-group}")
    private String riskConsumerGroupId;

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

    private void validateConfiguration() {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            meterRegistry.counter("kafka.config.errors", "param", "bootstrapServers").increment();
            throw new IllegalStateException("Kafka bootstrap servers must be configured");
        }
    }

    // —————————————————————————————————————————————————————————————————————————————
    // 1) KafkaAdmin to auto-create topics
    // —————————————————————————————————————————————————————————————————————————————

    @Bean
    public KafkaAdmin kafkaAdmin() {
        validateConfiguration();
        Map<String, Object> props = new HashMap<>();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        KafkaAdmin admin = new KafkaAdmin(props);
        admin.setFatalIfBrokerNotAvailable(false);
        return admin;
    }

    @Bean
    public NewTopic sensorTopic() {
        log.info("Ensuring sensor topic '{}' exists", sensorTopic);
        return new NewTopic(sensorTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic maintenanceRiskTopic() {
        log.info("Ensuring risk topic '{}' exists", maintenanceRiskTopic);
        return new NewTopic(maintenanceRiskTopic, 3, (short) 1);
    }

    // —————————————————————————————————————————————————————————————————————————————
    // 2) String-based Producer (for simple messages)
    // —————————————————————————————————————————————————————————————————————————————

    @Bean
    public Map<String, Object> stringProducerConfigs() {
        validateConfiguration();
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

    // —————————————————————————————————————————————————————————————————————————————
    // 3) RiskEvent Producer (JSON)
    // —————————————————————————————————————————————————————————————————————————————

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

    @Bean
    public KafkaSender<String, MaintenanceRiskEvent> reactiveRiskEventKafkaSender() {
        return KafkaSender.create(SenderOptions.create(riskEventProducerConfigs()));
    }

    // —————————————————————————————————————————————————————————————————————————————
    // 4) Spring‐Kafka Consumer (for @KafkaListener use)
    // —————————————————————————————————————————————————————————————————————————————

    @Bean
    public Map<String, Object> consumerConfigs() {
        validateConfiguration();
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,   bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,            sensorConsumerGroupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,   autoOffsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,    maxPollRecords);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,  enableAutoCommit);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG,     isolationLevel);
        return props;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean(name = "stringKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.setBatchListener(true);
        return factory;
    }

    // —————————————————————————————————————————————————————————————————————————————
    // 5) RiskEvent Consumer (for @KafkaListener of MaintenanceRiskEvent)
    // —————————————————————————————————————————————————————————————————————————————

    @Bean
    public Map<String, Object> riskConsumerConfigs() {
        Map<String, Object> props = new HashMap<>(consumerConfigs());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, riskConsumerGroupId);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MaintenanceRiskEvent.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES,    "com.banenor.events");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return props;
    }

    @Bean
    public ConsumerFactory<String, MaintenanceRiskEvent> riskConsumerFactory() {
        JsonDeserializer<MaintenanceRiskEvent> deserializer =
                new JsonDeserializer<>(MaintenanceRiskEvent.class, false)
                        .trustedPackages("com.banenor.events")
                        .ignoreTypeHeaders();
        return new DefaultKafkaConsumerFactory<>(riskConsumerConfigs(),
                new StringDeserializer(), deserializer);
    }

    @Bean(name = "riskKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, MaintenanceRiskEvent> riskKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, MaintenanceRiskEvent>();
        factory.setConsumerFactory(riskConsumerFactory());
        factory.setConcurrency(3);
        factory.setBatchListener(false);
        return factory;
    }

    // —————————————————————————————————————————————————————————————————————————————
    // 6) Reactor-Kafka Receiver for SensorMeasurementDTO
    // —————————————————————————————————————————————————————————————————————————————

    @Bean
    public KafkaReceiver<String, SensorMeasurementDTO> sensorDataKafkaReceiver() {
        validateConfiguration();

        // Base consumer props, overriding deserializer
        Map<String, Object> props = new HashMap<>(consumerConfigs());
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SensorMeasurementDTO.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES,    "com.banenor.dto");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        JsonDeserializer<SensorMeasurementDTO> sensorDeserializer =
                new JsonDeserializer<>(SensorMeasurementDTO.class, false)
                        .trustedPackages("com.banenor.dto");

        ReceiverOptions<String, SensorMeasurementDTO> opts =
                ReceiverOptions.<String, SensorMeasurementDTO>create(props)
                        .withKeyDeserializer(new StringDeserializer())
                        .withValueDeserializer(sensorDeserializer)
                        .subscription(Collections.singletonList(sensorTopic));

        return KafkaReceiver.create(opts);
    }
}
