package com.banenor.service;

import com.banenor.dto.DerailmentRiskDTO;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;

/**
 * Service interface for analyzing derailment risk based on sensor measurements.
 */
public interface DerailmentRiskService {

    /**
     * Retrieves and analyzes sensor data to evaluate derailment risk.
     * This method fetches sensor data for the given train number and time range,
     * and applies threshold checks on vibration measurements and time delay differences.
     * @param trainNo the train number.
     * @param start   the start time (inclusive) for data retrieval.
     * @param end     the end time (inclusive) for data retrieval.
     * @return a Flux stream of {@code DerailmentRiskDTO} objects representing the analysis.
     */
    Flux<DerailmentRiskDTO> fetchDerailmentRiskData(Integer trainNo, LocalDateTime start, LocalDateTime end);
}
