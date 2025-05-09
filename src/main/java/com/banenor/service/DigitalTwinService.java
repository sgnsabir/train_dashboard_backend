// src/main/java/com/banenor/service/DigitalTwinService.java
package com.banenor.service;

import com.banenor.dto.DigitalTwinDTO;
import com.banenor.dto.SensorMetricsDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Service interface for Digital Twin integration.
 * Now produces streams of DigitalTwinDTO.
 */
public interface DigitalTwinService {

    /**
     * Ingest new sensor metrics into the twin.
     */
    Mono<Void> updateTwin(SensorMetricsDTO metrics);

    /**
     * Fetch the latest insight for a single asset.
     */
    Mono<DigitalTwinDTO> getTwinState(Integer assetId);

    /**
     * Query a paged stream of insights by arbitrary filters.
     */
    Flux<DigitalTwinDTO> findTwinsByFilters(Map<String, Object> filters, int page, int size);

    /**
     * Count total insight records matching filters.
     */
    Mono<Long> countTwinsByFilters(Map<String, Object> filters);
}
