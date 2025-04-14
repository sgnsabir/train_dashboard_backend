package com.banenor.service;

import com.banenor.dto.DecisionDTO;
import com.banenor.dto.SensorMetricsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * A proof-of-concept implementation of a reinforcement learning service.
 * Currently uses simple rule-based logic to decide whether immediate maintenance is required.
 *
 * <p>
 * Replace the rule-based logic with your RL algorithm or integrate with an external RL service
 * as needed.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultReinforcementLearningService implements ReinforcementLearningService {

    // These thresholds could be made configurable in a future iteration.
    private static final double HIGH_SPEED_THRESHOLD = 100.0;
    private static final double HIGH_AOA_THRESHOLD = 7.0;
    private static final double HIGH_VIBRATION_THRESHOLD = 5.0;

    /**
     * Determines an adaptive maintenance decision based on sensor metrics.
     * This stub implementation uses simple rules:
     *
     * - If average speed and angle-of-attack exceed defined thresholds, then recommend immediate maintenance.
     * - Otherwise, recommend monitoring the asset.
     *
     * The confidence is computed as a normalized score from the deviation above thresholds.
     *
     * @param metrics the aggregated sensor metrics
     * @return a Mono emitting the decision as a DecisionDTO
     */
    @Override
    public Mono<DecisionDTO> decideMaintenanceAction(SensorMetricsDTO metrics) {
        return Mono.fromCallable(() -> {
            if (metrics == null) {
                log.warn("Received null sensor metrics; defaulting to no action.");
                return DecisionDTO.builder()
                        .decision("No Action")
                        .confidence(0.0)
                        .message("No sensor metrics available; unable to compute decision.")
                        .decisionTime(LocalDateTime.now())
                        .build();
            }

            double speed = (metrics.getAverageSpeed() != null) ? metrics.getAverageSpeed() : 0.0;
            double aoa = (metrics.getAverageAoa() != null) ? metrics.getAverageAoa() : 0.0;
            double vibration = (metrics.getAverageVibration() != null) ? metrics.getAverageVibration() : 0.0;

            boolean highSpeed = speed > HIGH_SPEED_THRESHOLD;
            boolean highAoa = aoa > HIGH_AOA_THRESHOLD;
            boolean highVibration = vibration > HIGH_VIBRATION_THRESHOLD;

            String decision;
            double confidence;
            StringBuilder message = new StringBuilder();

            if (highSpeed && highAoa && highVibration) {
                decision = "Schedule Immediate Maintenance";
                // Compute a confidence score proportional to the deviation
                confidence = Math.min(((speed - HIGH_SPEED_THRESHOLD) / 50.0 + (aoa - HIGH_AOA_THRESHOLD) / 5.0 + (vibration - HIGH_VIBRATION_THRESHOLD) / 2.0) / 3.0, 1.0);
                message.append(String.format("Speed (%.1f km/h), AOA (%.1f), and vibration (%.1f) are above thresholds. ", speed, aoa, vibration));
            } else {
                decision = "Monitor";
                confidence = 0.2; // Default low confidence when thresholds are not met.
                message.append("Sensor metrics are within acceptable limits.");
            }

            log.info("RL decision computed: {} with confidence {:.2f}", decision, confidence);

            return DecisionDTO.builder()
                    .decision(decision)
                    .confidence(confidence)
                    .message(message.toString().trim())
                    .decisionTime(LocalDateTime.now())
                    .build();
        }).onErrorResume(ex -> {
            log.error("Error during RL decision computation: {}", ex.getMessage(), ex);
            return Mono.just(DecisionDTO.builder()
                    .decision("No Action")
                    .confidence(0.0)
                    .message("Error computing RL decision: " + ex.getMessage())
                    .decisionTime(LocalDateTime.now())
                    .build());
        });
    }
}
