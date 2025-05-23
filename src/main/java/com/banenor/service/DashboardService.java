package com.banenor.service;

import com.banenor.dto.HistoricalDataResponse;
import com.banenor.dto.SensorMetricsDTO;
import com.banenor.dto.SystemDashboardDTO;
import reactor.core.publisher.Mono;

public interface DashboardService {
    Mono<SensorMetricsDTO> getLatestMetrics(Integer analysisId);
    Mono<HistoricalDataResponse> getHistoricalData(Integer analysisId);
    Mono<SystemDashboardDTO> getSystemDashboard();
}
