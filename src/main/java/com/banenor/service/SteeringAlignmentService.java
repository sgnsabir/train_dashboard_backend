package com.banenor.service;

import com.banenor.dto.SteeringAlignmentDTO;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

/**
 * Service interface for analyzing steering alignment based on sensor measurements.
 */
public interface SteeringAlignmentService {

    /**
     * Fetch and process sensor data to evaluate steering alignment.
     * The service computes whether the measured angle-of-attack (AOA) and the lateral-to-vertical force ratio
     * exceed the configured thresholds.
     *
     * @param trainNo the train number for which sensor data is to be retrieved.
     * @param start the start time for the query.
     * @param end the end time for the query.
     * @return a Flux of SteeringAlignmentDTO objects representing analyzed sensor records.
     */
    Flux<SteeringAlignmentDTO> fetchSteeringData(Integer trainNo, LocalDateTime start, LocalDateTime end);
}
