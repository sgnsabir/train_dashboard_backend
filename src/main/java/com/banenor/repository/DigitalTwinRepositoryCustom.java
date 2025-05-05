package com.banenor.repository;

import com.banenor.model.DigitalTwin;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Custom extension to allow truly dynamic‐column filtering,
 * plus total‐count for pagination.
 */
public interface DigitalTwinRepositoryCustom {

    /**
     * Find all twins matching the given filters, paged.
     *
     * @param filters map of column→value to filter by (only whitelisted columns)
     * @param page    zero‐based page index
     * @param size    page size
     */
    Flux<DigitalTwin> findByFilters(Map<String, Object> filters, int page, int size);

    /**
     * Count all twins matching the given filters.
     *
     * @param filters same map used in {@link #findByFilters}
     */
    Mono<Long> countByFilters(Map<String, Object> filters);
}
