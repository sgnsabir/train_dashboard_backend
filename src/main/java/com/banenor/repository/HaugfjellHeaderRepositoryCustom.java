package com.banenor.repository;

import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Custom repository interface for header + axle combined analytics.
 */
public interface HaugfjellHeaderRepositoryCustom {

    /**
     * Fetch header records joined with dynamically-aggregated axle metrics.
     *
     * @param trainNo    the train number to filter headers
     * @param axleStart  lower bound for axle.created_at
     * @param axleEnd    upper bound for axle.created_at
     * @param metrics    list of metric keys (e.g. "spd","aoa","vvibl",...)
     * @param page       zero-based page index
     * @param size       page size
     * @return a stream of rows as maps: header fields + avg_<metric> entries
     */
    Flux<Map<String, Object>> findHeaderWithAxleAggregates(
            Integer trainNo,
            LocalDateTime axleStart,
            LocalDateTime axleEnd,
            List<String> metrics,
            int page,
            int size
    );
}
