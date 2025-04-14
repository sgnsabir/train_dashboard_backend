package com.banenor.ai;

import com.banenor.dto.SensorMetricsDTO;
import reactor.core.publisher.Mono;

public interface AIPredictiveMaintenanceService {
    /**
     * Given sensor metrics, obtain an AI-based prediction for maintenance.
     *
     * @param metrics aggregated sensor metrics
     * @return a reactive Mono containing the prediction message
     */
    Mono<String> getPrediction(SensorMetricsDTO metrics);
}
