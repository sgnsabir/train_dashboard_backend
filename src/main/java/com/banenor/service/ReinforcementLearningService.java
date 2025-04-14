package com.banenor.service;

import com.banenor.dto.DecisionDTO;
import com.banenor.dto.SensorMetricsDTO;
import reactor.core.publisher.Mono;

/**
 * Service interface for adaptive maintenance decision making using Reinforcement Learning.
 */
public interface ReinforcementLearningService {

    /**
     * Given aggregated sensor metrics, compute an adaptive maintenance decision.
     *
     * @param metrics the aggregated sensor metrics
     * @return a Mono emitting a DecisionDTO containing the recommended decision, confidence, and message
     */
    Mono<DecisionDTO> decideMaintenanceAction(SensorMetricsDTO metrics);
}
