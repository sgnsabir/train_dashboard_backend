package com.banenor.service;

import com.banenor.dto.SegmentAnalysisDTO;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

/**
 * Groups sensor records by track segment and computes anomaly counts.
 */
public interface SegmentAnalysisService {

    /**
     * Perform segment-based analysis.
     *
     * @param trainNo the train number
     * @param start   analysis start time
     * @param end     analysis end time
     * @return a Flux of per‑segment analysis results
     */
    Flux<SegmentAnalysisDTO> analyzeSegmentData(Integer trainNo, LocalDateTime start, LocalDateTime end);
}
