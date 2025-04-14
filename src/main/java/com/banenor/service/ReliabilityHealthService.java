package com.banenor.service;

import com.banenor.dto.ReliabilityHealthDTO;
import reactor.core.publisher.Mono;

/**
 * Service interface for calculating the overall reliability health of a train.
 */
public interface ReliabilityHealthService {

    /**
     * Calculates the overall health score for a given train based on its latest sensor metrics.
     *
     * @param trainNo the train number
     * @return a Mono emitting the ReliabilityHealthDTO containing the health score and related data
     */
    Mono<ReliabilityHealthDTO> calculateHealthScore(Integer trainNo);
}
