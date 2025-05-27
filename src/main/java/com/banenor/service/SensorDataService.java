// src/main/java/com/banenor/service/SensorDataService.java
package com.banenor.service;

import com.banenor.dto.SensorAggregationDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface SensorDataService {
    /** 1) Ingest a raw JSON‐string sensor message */
    Mono<Void> processSensorData(String message);

    /** 2) Aggregate over an explicit time window */
    Mono<Void> aggregateSensorDataByRange(LocalDateTime from, LocalDateTime to);

    /** 3) Global, non‐time‐bounded aggregation */
    Mono<Void> aggregateSensorData();

    /** 4) Fetch the raw time‐series data in window */
    Flux<Object> getSensorDataByRange(LocalDateTime from, LocalDateTime to);

    /** 5a) Performance Index over entire span (MIN→MAX) */
    Mono<SensorAggregationDTO> getPerformance(Integer analysisId);

    /** 5b) Performance Index within an arbitrary window */
    Mono<SensorAggregationDTO> getPerformance(Integer analysisId,
                                              LocalDateTime from,
                                              LocalDateTime to);

}
