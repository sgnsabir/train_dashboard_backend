package com.banenor.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.util.HashMap;
import java.util.Map;

@Component
public class SensorDataProducer {

    private static final Logger logger = LoggerFactory.getLogger(SensorDataProducer.class);
    private final KafkaSender<String, String> kafkaSender;

    public SensorDataProducer() {
        // Configure sender properties
        Map<String, Object> senderProps = new HashMap<>();
        senderProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        senderProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        senderProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        SenderOptions<String, String> senderOptions = SenderOptions.create(senderProps);
        this.kafkaSender = KafkaSender.create(senderOptions);
    }

    public Mono<Void> sendSensorData(String topic, String message) {
        SenderRecord<String, String, String> record =
                SenderRecord.create(topic, null, null, null, message, null);
        return kafkaSender.send(Mono.just(record))
                .doOnNext(result -> logger.info("Sent sensor data to topic {}: {}", topic, message))
                .then();
    }
}
