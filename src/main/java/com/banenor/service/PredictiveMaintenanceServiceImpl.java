package com.banenor.service;

import java.time.LocalDateTime;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.banenor.ai.AIPredictiveMaintenanceService;
import com.banenor.config.MaintenanceProperties;
import com.banenor.dto.PredictiveMaintenanceDTO;
import com.banenor.dto.SensorDataDTO;
import com.banenor.events.MaintenanceRiskEvent;
import com.banenor.strategy.RiskCalculationStrategy;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PredictiveMaintenanceServiceImpl implements PredictiveMaintenanceService {

    private final DashboardService dashboardService;
    private final MaintenanceProperties maintenanceProperties;
    private final RiskCalculationStrategy riskCalculationStrategy;
    private final AIPredictiveMaintenanceService aiPredictiveMaintenanceService;
    private final KafkaTemplate<String, MaintenanceRiskEvent> kafkaTemplate;
    private final CircuitBreaker circuitBreaker;

    public PredictiveMaintenanceServiceImpl(DashboardService dashboardService,
                                            MaintenanceProperties maintenanceProperties,
                                            RiskCalculationStrategy riskCalculationStrategy,
                                            AIPredictiveMaintenanceService aiPredictiveMaintenanceService,
                                            KafkaTemplate<String, MaintenanceRiskEvent> kafkaTemplate,
                                            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.dashboardService = dashboardService;
        this.maintenanceProperties = maintenanceProperties;
        this.riskCalculationStrategy = riskCalculationStrategy;
        this.aiPredictiveMaintenanceService = aiPredictiveMaintenanceService;
        this.kafkaTemplate = kafkaTemplate;
        // Obtain a circuit breaker instance for the AI predictive service
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiPredictiveMaintenance");
    }

    @Override
    public Mono<Double> calculateRiskScore(Integer analysisId) {
        // Calculate risk score and publish event if above threshold.
        return dashboardService.getLatestMetrics(analysisId)
                .map(metrics -> {
                    double riskScore = riskCalculationStrategy.calculateRisk(metrics, maintenanceProperties);
                    if (riskScore >= maintenanceProperties.getRiskScoreThreshold()) {
                        MaintenanceRiskEvent event = MaintenanceRiskEvent.builder()
                                .analysisId(analysisId)
                                .riskScore(riskScore)
                                .timestamp(LocalDateTime.now())
                                .build();
                        kafkaTemplate.send("maintenance-risk-events", event);
                        log.info("Published maintenance risk event for analysisId {}: riskScore={}", analysisId, riskScore);
                    }
                    return riskScore;
                });
    }

    @Override
    public Mono<PredictiveMaintenanceDTO> getMaintenanceAnalysis(Integer analysisId, String alertEmail) {
        // Compose reactive calls with circuit breaker protection around the AI prediction call.
        return dashboardService.getLatestMetrics(analysisId)
                .flatMap(metrics ->
                        calculateRiskScore(analysisId)
                                .flatMap(riskScore ->
                                        // Wrap the call to the AI predictive service with a circuit breaker operator.
                                        aiPredictiveMaintenanceService.getPrediction(metrics)
                                                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                                                .defaultIfEmpty("AI prediction not configured.")
                                                .map(aiPrediction -> {
                                                    PredictiveMaintenanceDTO response = new PredictiveMaintenanceDTO();
                                                    response.setAnalysisId(analysisId);
                                                    response.setAverageSpeed(metrics.getAverageSpeed());
                                                    response.setSpeedVariance(metrics.getSpeedVariance());
                                                    response.setAverageAoa(metrics.getAverageAoa());
                                                    response.setAverageVibration(metrics.getAverageVibration());
                                                    response.setAverageVerticalForce(metrics.getAverageVerticalForce());
                                                    response.setAverageLateralForce(metrics.getAverageLateralForce());
                                                    response.setAverageLateralVibration(metrics.getAverageLateralVibration());
                                                    response.setRiskScore(riskScore);
                                                    String basicMessage = (riskScore >= maintenanceProperties.getRiskScoreThreshold())
                                                            ? "High risk detected: Risk score = " + riskScore + ". Immediate maintenance required."
                                                            : "Equipment operating normally. Risk score = " + riskScore + ".";
                                                    response.setPredictionMessage(basicMessage + " | " + aiPrediction);
                                                    return response;
                                                })
                                )
                );
    }

    @Override
    public Mono<String> analyzeMaintenanceRisk(Integer analysisId, String alertEmail) {
        return getMaintenanceAnalysis(analysisId, alertEmail)
                .map(PredictiveMaintenanceDTO::getPredictionMessage);
    }

    @Override
    public Mono<Void> processMaintenanceData(SensorDataDTO sensorData) {
        return calculateRiskScore(sensorData.getTrainNo())
                .flatMap(riskScore -> {
                    if (riskScore >= maintenanceProperties.getRiskScoreThreshold()) {
                        return analyzeMaintenanceRisk(sensorData.getTrainNo(), "alerts@banenor.com")
                                .then();
                    }
                    return Mono.empty();
                });
    }
}
