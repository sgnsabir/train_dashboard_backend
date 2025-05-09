package com.banenor.service;

import reactor.core.publisher.Mono;

public interface SensorDataAggregationService {
    Mono<Void> aggregateSensorDataByRange(String startDate, String endDate);
    Mono<Void> aggregateSensorData();
}
