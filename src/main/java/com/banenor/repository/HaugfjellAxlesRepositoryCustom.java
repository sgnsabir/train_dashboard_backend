package com.banenor.repository;

import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Common interface for dynamic, paginated, per-VIT aggregations.
 */
public interface HaugfjellAxlesRepositoryCustom {

    /**
     * Compute per-VIT aggregations (avg/min/max) over a single TP column,
     * with pagination.
     *
     * @param trainNo the train number to filter.
     * @param start   inclusive lower bound of created_at.
     * @param end     inclusive upper bound of created_at.
     * @param column  the single column to aggregate (must be whitelisted).
     * @param offset  zero-based row offset.
     * @param limit   max rows to return.
     * @return Flux of maps, each with keys: "vit", "avg", "min", "max".
     */
    Flux<Map<String, Object>> findDynamicAggregationsByTrain(
            Integer trainNo,
            LocalDateTime start,
            LocalDateTime end,
            String column,
            int offset,
            int limit
    );
}
