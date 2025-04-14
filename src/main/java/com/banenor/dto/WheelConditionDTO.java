package com.banenor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Wheel Condition Analysis.
 * This DTO encapsulates sensor measurements used to detect potential wheel flat conditions.
 * It includes the train number, measurement timestamp, vertical vibration values (left and right),
 * a flag indicating if a wheel flat condition is suspected, and an anomaly message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WheelConditionDTO {
    /**
     * Train number associated with the sensor record.
     */
    private Integer trainNo;

    /**
     * Timestamp when the sensor measurement was recorded.
     */
    private LocalDateTime measurementTime;

    /**
     * Measured vertical vibration on the left side.
     */
    private Double verticalVibrationLeft;

    /**
     * Measured vertical vibration on the right side.
     */
    private Double verticalVibrationRight;

    /**
     * Flag indicating if a potential wheel flat is suspected.
     */
    private Boolean suspectedWheelFlat;

    /**
     * A descriptive message detailing any anomaly detected in the wheel condition.
     */
    private String anomalyMessage;
}
