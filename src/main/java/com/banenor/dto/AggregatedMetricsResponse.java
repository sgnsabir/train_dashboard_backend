package com.banenor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing aggregated metrics from cache.
 */
@Data
@NoArgsConstructor
public class AggregatedMetricsResponse {
    /**
     * The cached average value, if available.
     */
    private Double average;

    /**
     * Error or fallback message when no cached average is available.
     */
    private String message;

    public AggregatedMetricsResponse(Double average) {
        this.average = average;
        this.message = null;
    }

    public AggregatedMetricsResponse(String message) {
        this.average = null;
        this.message = message;
    }
}
