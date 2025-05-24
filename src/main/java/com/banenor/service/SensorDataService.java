package com.banenor.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface SensorDataService {
    Mono<Void> processSensorData(String message);

    Mono<Void> aggregateSensorDataByRange(String startDate, String endDate);

    Mono<Void> aggregateSensorData();

    Flux<Object> getSensorDataByRange(LocalDateTime from, LocalDateTime to);
}
