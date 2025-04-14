package com.banenor.strategy;

import com.banenor.config.MaintenanceProperties;
import com.banenor.dto.SensorMetricsDTO;

public interface RiskCalculationStrategy {
    /**
     * Calculate a risk score based on sensor metrics and configured thresholds/weights.
     *
     * @param metrics    the aggregated sensor metrics DTO
     * @param properties the maintenance properties with thresholds and weights
     * @return the calculated risk score
     */
    double calculateRisk(SensorMetricsDTO metrics, MaintenanceProperties properties);
}
