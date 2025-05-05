// src/main/java/com/banenor/repository/TrainHealthRepositoryCustom.java
package com.banenor.repository;

import com.banenor.model.TrainHealth;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Custom extension to support dynamic‐column filters on train health records.
 */
public interface TrainHealthRepositoryCustom {
    /**
     * Retrieve TrainHealth rows matching arbitrary column‐value filters.
     *
     * @param filters Map of column name → desired value
     * @return Flux of matching TrainHealth entities
     */
    Flux<TrainHealth> findByDynamicFilters(Map<String, Object> filters);
}
