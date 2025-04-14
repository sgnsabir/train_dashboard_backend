package com.banenor.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for insights thresholds used in domain services.
 * This class holds all the threshold values for sensor data analysis,
 * such as for steering alignment, derailment risk, wheel condition,
 * and track condition. These values are defined under the prefix "insights"
 * in your application configuration (e.g., application.yml). Adjust these values
 * at runtime if needed (for example via Spring Cloud Config).
 */
@Configuration
@ConfigurationProperties(prefix = "insights")
@Data
public class InsightsProperties {

    @NotNull
    private SteeringConfig steering = new SteeringConfig();

    @NotNull
    private DerailmentConfig derailment = new DerailmentConfig();

    @NotNull
    private WheelConfig wheel = new WheelConfig();

    @NotNull
    private TrackConfig track = new TrackConfig();

    /**
     * Configuration for steering alignment thresholds.
     */
    @Data
    public static class SteeringConfig {
        /**
         * Threshold for absolute angle-of-attack.
         * If |AOA| exceeds this value, the system flags a steering misalignment.
         */
        private double aoaThreshold = 5.0;

        /**
         * Threshold for the lateral-to-vertical force ratio.
         * If the computed ratio exceeds this value, it indicates potential steering misalignment.
         */
        private double lvRatioThreshold = 0.8;
    }

    /**
     * Configuration for derailment risk thresholds.
     */
    @Data
    public static class DerailmentConfig {
        /**
         * Threshold for vibration spikes.
         * Exceeding this threshold could indicate a derailment risk.
         */
        private double vibrationThreshold = 100.0;

        /**
         * Threshold for time delay differences between sensors.
         * If the difference exceeds this value, a derailment risk may be flagged.
         */
        private double timeDelayThreshold = 0.02;
    }

    /**
     * Configuration for wheel condition analysis.
     */
    @Data
    public static class WheelConfig {
        /**
         * Vibration threshold indicating potential wheel flat conditions.
         */
        private double vibrationFlatThreshold = 120.0;
    }

    /**
     * Configuration for track condition analysis.
     */
    @Data
    public static class TrackConfig {
        /**
         * Threshold for lateral force that can indicate high track stress.
         */
        private double highLateralForce = 200.0;

        /**
         * Threshold for vertical force that can indicate high track stress.
         */
        private double highVerticalForce = 450.0;
    }
}
