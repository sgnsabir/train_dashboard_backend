package com.banenor.service;

import org.springframework.stereotype.Service;

import com.banenor.dto.PredictiveMaintenanceResponse;
import com.banenor.dto.SensorDataDTO;

import reactor.core.publisher.Mono;

@Service
public interface PredictiveMaintenanceService {
    Mono<Double> calculateRiskScore(Integer analysisId);
    Mono<String> analyzeMaintenanceRisk(Integer analysisId, String alertEmail);
    Mono<PredictiveMaintenanceResponse> getMaintenanceAnalysis(Integer analysisId, String alertEmail);
    
    /**
     * Process incoming sensor data for predictive maintenance analysis.
     * 
     * @param sensorData The sensor data to process
     * @return A Mono that completes when processing is done
     */
    Mono<Void> processMaintenanceData(SensorDataDTO sensorData);
}
