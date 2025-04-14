package com.banenor.service;

import reactor.core.publisher.Mono;

public interface SensorDataAggregationService {
    Mono<Void> aggregateSensorData();
}
