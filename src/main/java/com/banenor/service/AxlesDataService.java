package com.banenor.service;

import com.banenor.dto.AxlesDataDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Service for fetching per‐TP axles data streams and global aggregations.
 */
public interface AxlesDataService {

    /**
     * Stream out each per‐TP reading as a DTO, for a given train and time window.
     */
    Flux<AxlesDataDTO> getAxlesData(Integer trainNo,
                                    LocalDateTime start,
                                    LocalDateTime end,
                                    String measurementPoint);

    /**
     * Compute a single DTO of aggregated (e.g. average) values
     * for the given test‐point across all records.
     */
    Mono<AxlesDataDTO> getGlobalAggregations(String measurementPoint);

}
