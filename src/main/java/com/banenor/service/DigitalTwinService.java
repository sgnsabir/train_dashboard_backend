package com.banenor.service;

import com.banenor.dto.CameraPose;
import com.banenor.dto.DigitalTwinDTO;
import com.banenor.dto.SensorMetricsDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface DigitalTwinService {
    Mono<Void> updateTwin(SensorMetricsDTO metrics);
    Mono<CameraPose> getTwinState(Integer assetId);
    Flux<CameraPose> streamDigitalTwinUpdates(Integer assetId);
    Flux<DigitalTwinDTO> findTwinsByFilters(Map<String, Object> filters, int page, int size);
    Mono<Long> countTwinsByFilters(Map<String, Object> filters);
}
