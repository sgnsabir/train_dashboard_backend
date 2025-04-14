package com.banenor.service;

import com.banenor.dto.HistoricalDataResponse;
import com.banenor.dto.SensorMetricsDTO;
import reactor.core.publisher.Mono;

public interface DashboardService {
    Mono<SensorMetricsDTO> getLatestMetrics(Integer analysisId);
    Mono<HistoricalDataResponse> getHistoricalData(Integer analysisId);
}
