package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for RL-based maintenance decisions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionDTO {
    /**
     * The recommended maintenance decision (e.g., "Schedule Immediate Maintenance",
     * "Monitor", etc.).
     */
    private String decision;

    /**
     * The expected outcome or confidence score (e.g., a value between 0.0 and 1.0).
     */
    private double confidence;

    /**
     * A descriptive message explaining the rationale behind the decision.
     */
    private String message;

    /**
     * The timestamp when the decision was generated.
     */
    private LocalDateTime decisionTime;
}
