// src/main/java/com/banenor/service/TrainHealthService.java
package com.banenor.service;

import com.banenor.dto.TrainHealthDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Service interface for per-train health metrics.
 */
public interface TrainHealthService {

    /**
     * Fetch all health records for a train, newest first.
     */
    Flux<TrainHealthDTO> getHealthForTrain(Integer trainNo);

    /**
     * Fetch the latest health record for a train.
     */
    Mono<TrainHealthDTO> getLatestHealth(Integer trainNo);

    /**
     * Query health records by arbitrary column filters (column→value).
     */
    Flux<TrainHealthDTO> findByFilters(Map<String, Object> filters);
}
