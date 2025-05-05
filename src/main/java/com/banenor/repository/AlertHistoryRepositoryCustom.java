package com.banenor.repository;

import com.banenor.model.AlertHistory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Custom repository operations for AlertHistory:
 *   - findByFilters: dynamic WHERE‐clause + paging
 *   - countByFilters: how many total match (for UI pagination)
 */
public interface AlertHistoryRepositoryCustom {

    /**
     * Find a page of alerts matching the given filters.
     *
     * @param filters a map of column → value constraints. Supported keys:
     *                "acknowledged" → Boolean,
     *                "from"          → java.time.LocalDateTime,
     *                "to"            → java.time.LocalDateTime,
     *                "subjectContains" → String (partial match)
     * @param page    zero‐based page index
     * @param size    maximum rows per page
     */
    Flux<AlertHistory> findByFilters(Map<String, Object> filters, int page, int size);

    /**
     * Count how many alerts match the given filters.
     */
    Mono<Long> countByFilters(Map<String, Object> filters);
}
