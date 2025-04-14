package com.banenor.service;

import com.banenor.config.MaintenanceProperties;
import com.banenor.dto.ReliabilityHealthDTO;
import com.banenor.dto.SensorMetricsDTO;
import com.banenor.strategy.RiskCalculationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Implementation of ReliabilityHealthService.
 *
 * This service retrieves the latest aggregated sensor metrics for a given train using DashboardService,
 * calculates a risk score via RiskCalculationStrategy, and then computes a health score defined as:
 *   healthScore = 100 - (riskScore * 100)
 * A descriptive message is returned along with the computed score.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('MAINTENANCE')")
public class ReliabilityHealthServiceImpl implements ReliabilityHealthService {

    private final DashboardService dashboardService;
    private final RiskCalculationStrategy riskCalculationStrategy;
    private final MaintenanceProperties maintenanceProperties;

    @Override
    public Mono<ReliabilityHealthDTO> calculateHealthScore(Integer trainNo) {
        return dashboardService.getLatestMetrics(trainNo)
                .flatMap((SensorMetricsDTO metrics) -> {
                    double riskScore = riskCalculationStrategy.calculateRisk(metrics, maintenanceProperties);
                    double healthScore = Math.max(0.0, 100.0 - (riskScore * 100.0));

                    String message = (healthScore >= 80) ? "Excellent health" :
                            (healthScore >= 60) ? "Good health" :
                                    (healthScore >= 40) ? "Moderate health" : "Poor health, immediate maintenance required";

                    ReliabilityHealthDTO dto = ReliabilityHealthDTO.builder()
                            .trainNo(trainNo)
                            .healthScore(healthScore)
                            .message(message)
                            .measurementTime(metrics.getCreatedAt())
                            .build();

                    log.info("Calculated health for train {}: healthScore={} (riskScore={})", trainNo, healthScore, riskScore);
                    return Mono.just(dto);
                })
                .doOnError(ex -> log.error("Error calculating health score for train {}: {}", trainNo, ex.getMessage(), ex));
    }
}
