package com.banenor.service;

import com.banenor.dto.SensorMetricsDTO;
import com.banenor.dto.DigitalTwinDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Service interface for Digital Twin integration.
 * Provides methods to update and retrieve the digital twin state for train assets.
 */
public interface DigitalTwinService {

    /**
     * Updates the digital twin state for a given asset based on the latest sensor metrics.
     *
     * @param metrics Aggregated sensor metrics.
     * @return a Mono signaling completion.
     */
    Mono<Void> updateTwin(SensorMetricsDTO metrics);

    /**
     * Retrieves the current digital twin state for the specified asset.
     *
     * @param assetId the asset identifier (e.g., train number).
     * @return a Mono emitting the VirtualAssetDTO representing the current state.
     */
    Mono<DigitalTwinDTO> getTwinState(Integer assetId);

    Flux<DigitalTwinDTO> findTwinsByFilters(Map<String, Object> filters, int page, int size);
    Mono<Long> countTwinsByFilters(Map<String, Object> filters);
}
