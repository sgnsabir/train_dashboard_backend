package com.banenor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized maintenance configuration.
 * Annotated with @RefreshScope so that changes (via config server or actuator refresh)
 * are applied at runtime without redeployment.
 */
@Data
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "maintenance")
public class MaintenanceProperties {

    // Predictive Maintenance Thresholds (for calculations)
    private double speedThreshold = 80.0;
    private double aoaThreshold = 5.0;
    private double vibrationThreshold = 3.0;
    private double verticalForceThreshold = 500.0;
    private double lateralForceThreshold = 300.0;
    private double lateralVibrationThreshold = 2.0;
    private double accelerationThreshold = 5.0; // Optional
    private double axleLoadThreshold = 1000.0;

    // Weights for risk score calculation
    private double weightSpeed = 0.20;
    private double weightAoa = 0.10;
    private double weightVibration = 0.10;
    private double weightVerticalForce = 0.10;
    private double weightLateralForce = 0.10;
    private double weightLateralVibration = 0.10;
    private double weightAcceleration = 0.15;
    private double weightAxleLoad = 0.10;

    // Overall risk threshold to decide if maintenance is required
    private double riskScoreThreshold = 0.7;

    // Realtime Alert Thresholds (for immediate alerts)
    private double realtimeSpeedThreshold = 100.0;
    private double realtimeVibrationLeftThreshold = 5.0;
    private double realtimeVibrationRightThreshold = 5.0;
    private double realtimeVerticalForceRightThreshold = 550.0;
    private double realtimeVerticalForceLeftThreshold = 550.0;
    private double realtimeLateralForceRightThreshold = 350.0;
    private double realtimeLateralVibrationRightThreshold = 2.5;
}
