package com.banenor.service;

import reactor.core.publisher.Mono;

public interface SensorDataService {
    Mono<Void> processSensorData(String message);
}
