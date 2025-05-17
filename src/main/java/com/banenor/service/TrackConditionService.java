package com.banenor.service;

import com.banenor.dto.TrackConditionDTO;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;

/**
 * Service interface for analyzing track conditions based on sensor measurements.
 * The service evaluates lateral and vertical forces from sensor data to detect potential track stress or geometry issues.
 */
public interface TrackConditionService {

    /**
     * Retrieves and analyzes sensor data for track conditions.
     *
     * @param trainNo the train number.
     * @param start   the start time (inclusive) for sensor data retrieval.
     * @param end     the end time (inclusive) for sensor data retrieval.
     * @return a Flux stream of {@code TrackConditionDTO} objects representing the track condition analysis.
     */
    Flux<TrackConditionDTO> fetchTrackConditionData(Integer trainNo, LocalDateTime start, LocalDateTime end);
}
