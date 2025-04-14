package com.banenor.service;

import com.banenor.dto.PerformanceDTO;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

/**
 * Service interface for retrieving performance metrics (e.g., speed and acceleration)
 * from sensor records within a specified date range. Implementations are expected to
 * default to the last 7 days if the date parameters are not provided.
 * This interface allows for a flexible, testable, and production-ready implementation
 * using WebFlux and R2DBC.
 */
public interface PerformanceService {

    /**
     * Retrieves performance data (e.g. speed and acceleration) from sensor records
     * within the specified date range. If the start or end dates are not provided,
     * implementations should default to the last 7 days.
     *
     * @param start the starting LocalDateTime boundary (inclusive)
     * @param end   the ending LocalDateTime boundary (inclusive)
     * @return a Flux of PerformanceDTO objects representing the performance metrics
     */
    Flux<PerformanceDTO> getPerformanceData(LocalDateTime start, LocalDateTime end);
}
