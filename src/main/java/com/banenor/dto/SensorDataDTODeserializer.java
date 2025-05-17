package com.banenor.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import java.util.Map;

/**
 * A custom deserializer for SensorDataDTO.
 * This implementation uses Jackson to convert the incoming JSON bytes
 * into a SensorDataDTO instance. It is production‑ready, robust,
 * and suitable for use in reactive Kafka consumers.
 */
public class SensorDataDTODeserializer implements Deserializer<SensorDataDTO> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // Optionally, can configure the ObjectMapper here if needed.
    }

    @Override
    public SensorDataDTO deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.readValue(data, SensorDataDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing SensorDataDTO from topic " + topic, e);
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}
