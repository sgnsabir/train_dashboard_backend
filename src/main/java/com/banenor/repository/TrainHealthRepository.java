// src/main/java/com/banenor/repository/TrainHealthRepository.java
package com.banenor.repository;

import com.banenor.model.TrainHealth;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * R2DBC repository for per-train health metrics.
 */
public interface TrainHealthRepository
        extends R2dbcRepository<TrainHealth, Integer>, TrainHealthRepositoryCustom {

    /**
     * Stream all health records for a given train, newest first.
     */
    Flux<TrainHealth> findAllByTrainNoOrderByTimestampDesc(Integer trainNo);

    /**
     * Get the latest health record for a given train.
     */
    Mono<TrainHealth> findTopByTrainNoOrderByTimestampDesc(Integer trainNo);
}
