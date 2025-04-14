package com.banenor.service;

import com.banenor.dto.WheelConditionDTO;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;

/**
 * Service interface for analyzing wheel condition based on sensor data.
 */
public interface WheelConditionService {

    /**
     * Retrieves and analyzes sensor data to evaluate wheel condition.
     * Flags a potential wheel flat if either vertical vibration measurement exceeds
     * the configured threshold.
     * @param trainNo the train number.
     * @param start the start time (inclusive) for sensor data retrieval.
     * @param end the end time (inclusive) for sensor data retrieval.
     * @return a Flux stream of WheelConditionDTO objects representing the analysis.
     */
    Flux<WheelConditionDTO> fetchWheelConditionData(Integer trainNo, LocalDateTime start, LocalDateTime end);
}
