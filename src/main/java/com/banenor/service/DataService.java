package com.banenor.service;

import com.banenor.dto.RawDataResponse;
import com.banenor.dto.SensorTpSeriesDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface DataService {
    Mono<Void> processSensorData(String message);
    Flux<RawDataResponse> getRawData(Integer analysisId, String sensorType, int page, int size);
    Flux<RawDataResponse> getDetailedSensorData(Integer analysisId, int page, int size);
    Mono<SensorTpSeriesDTO> tpSeries(Integer analysisId,
                                     String station,
                                     LocalDateTime start,
                                     LocalDateTime end,
                                     String sensor);
}
