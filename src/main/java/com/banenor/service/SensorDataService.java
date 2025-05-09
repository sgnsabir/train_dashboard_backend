package com.banenor.service;

import reactor.core.publisher.Mono;

public interface SensorDataService {
    Mono<Void> processSensorData(String message);

    Mono<Void> aggregateSensorDataByRange(String startDate, String endDate);

    Mono<Void> aggregateSensorData();
}
