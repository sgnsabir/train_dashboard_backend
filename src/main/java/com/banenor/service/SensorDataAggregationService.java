package com.banenor.service;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface SensorDataAggregationService {
    Mono<Void> aggregateSensorDataByRange(LocalDateTime startDate, LocalDateTime endDate);
    Mono<Void> aggregateSensorData();
}
