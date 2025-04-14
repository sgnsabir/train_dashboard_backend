package com.banenor.service;

import com.banenor.dto.RawDataResponse;
import com.banenor.dto.SensorMeasurementDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DataService {
    Mono<Void> processSensorData(String message);
    Flux<RawDataResponse> getRawData(Integer analysisId, String sensorType, int page, int size);
    Flux<SensorMeasurementDTO> getDetailedSensorData(Integer analysisId, int page, int size);
}
