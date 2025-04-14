package com.banenor.service;

import com.banenor.dto.SegmentAnalysisDTO;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;

/**
 * Service interface for performing segment-based analysis on sensor data.
 * This service groups sensor records by track segment and aggregates anomaly counts
 * to identify potential "hot spots" on the track.
 */
public interface SegmentAnalysisService {

    /**
     * Analyze sensor data grouped by track segment.
     *
     * @param trainNo the train number to analyze.
     * @param start   the start time (inclusive) for sensor data retrieval.
     * @param end     the end time (inclusive) for sensor data retrieval.
     * @return a Flux of SegmentAnalysisDTO containing aggregated anomaly data per segment.
     */
    Flux<SegmentAnalysisDTO> analyzeSegmentData(Integer trainNo, LocalDateTime start, LocalDateTime end);
}
