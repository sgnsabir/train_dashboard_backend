package com.banenor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * DTO representing aggregated metrics from cache.
 */
@Data
@NoArgsConstructor
public class AggregatedMetricsResponse {
    private Mono<Double> avgSpeed;
    private String error;

    public AggregatedMetricsResponse(Mono<Double> avgSpeed) {
        this.avgSpeed = avgSpeed;
        this.error = null;
    }

    public AggregatedMetricsResponse(String error) {
        this.error = error;
        this.avgSpeed = null;
    }
}
