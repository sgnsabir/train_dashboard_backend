package com.banenor.strategy;

import com.banenor.config.MaintenanceProperties;
import com.banenor.dto.SensorMetricsDTO;
import org.springframework.stereotype.Service;

@Service
public class DefaultRiskCalculationStrategy implements RiskCalculationStrategy {

    @Override
    public double calculateRisk(SensorMetricsDTO metrics, MaintenanceProperties properties) {
        if (metrics == null) {
            throw new IllegalArgumentException("Sensor metrics cannot be null");
        }

        // Speed score: (averageSpeed - threshold)/20 capped at 1
        double speedScore = (metrics.getAverageSpeed() != null && metrics.getAverageSpeed() > properties.getSpeedThreshold())
                ? Math.min((metrics.getAverageSpeed() - properties.getSpeedThreshold()) / 20.0, 1.0)
                : 0.0;

        // AOA score: (averageAoa - threshold)/5 capped at 1
        double aoaScore = (metrics.getAverageAoa() != null && metrics.getAverageAoa() > properties.getAoaThreshold())
                ? Math.min((metrics.getAverageAoa() - properties.getAoaThreshold()) / 5.0, 1.0)
                : 0.0;

        // Vibration score: average of left and right vertical vibrations; missing values treated as 0
        double leftVibration = (metrics.getAverageVibrationLeft() != null) ? metrics.getAverageVibrationLeft() : 0.0;
        double rightVibration = (metrics.getAverageVibrationRight() != null) ? metrics.getAverageVibrationRight() : 0.0;
        double avgVibration = (leftVibration + rightVibration) / 2.0;
        double vibrationScore = (avgVibration > properties.getVibrationThreshold())
                ? Math.min((avgVibration - properties.getVibrationThreshold()) / 2.0, 1.0)
                : 0.0;

        // Vertical force score: average of left and right; missing values treated as 0
        double leftVertical = (metrics.getAverageVerticalForceLeft() != null) ? metrics.getAverageVerticalForceLeft() : 0.0;
        double rightVertical = (metrics.getAverageVerticalForceRight() != null) ? metrics.getAverageVerticalForceRight() : 0.0;
        double avgVerticalForce = (leftVertical + rightVertical) / 2.0;
        double verticalForceScore = (avgVerticalForce > properties.getVerticalForceThreshold())
                ? Math.min((avgVerticalForce - properties.getVerticalForceThreshold()) / 100.0, 1.0)
                : 0.0;

        // Lateral force score: average of left and right; missing values treated as 0
        double leftLateralForce = (metrics.getAverageLateralForceLeft() != null) ? metrics.getAverageLateralForceLeft() : 0.0;
        double rightLateralForce = (metrics.getAverageLateralForceRight() != null) ? metrics.getAverageLateralForceRight() : 0.0;
        double avgLateralForce = (leftLateralForce + rightLateralForce) / 2.0;
        double lateralForceScore = (avgLateralForce > properties.getLateralForceThreshold())
                ? Math.min((avgLateralForce - properties.getLateralForceThreshold()) / 50.0, 1.0)
                : 0.0;

        // Lateral vibration score:
        // Individually fall back to vertical vibration (or 0.0) if missing.
        Double leftLatVib = metrics.getAverageLateralVibrationLeft();
        if (leftLatVib == null) {
            leftLatVib = (metrics.getAverageVibrationLeft() != null) ? metrics.getAverageVibrationLeft() : 0.0;
        }
        Double rightLatVib = metrics.getAverageLateralVibrationRight();
        if (rightLatVib == null) {
            rightLatVib = (metrics.getAverageVibrationRight() != null) ? metrics.getAverageVibrationRight() : 0.0;
        }
        double avgLateralVibration = (leftLatVib + rightLatVib) / 2.0;
        double lateralVibrationScore = (avgLateralVibration > properties.getLateralVibrationThreshold())
                ? Math.min((avgLateralVibration - properties.getLateralVibrationThreshold()) / 1.0, 1.0)
                : 0.0;

        // Axle load score: average of left and right axle loads; missing values treated as 0
        double leftAxleLoad = (metrics.getAverageAxleLoadLeft() != null) ? metrics.getAverageAxleLoadLeft() : 0.0;
        double rightAxleLoad = (metrics.getAverageAxleLoadRight() != null) ? metrics.getAverageAxleLoadRight() : 0.0;
        double avgAxleLoad = (leftAxleLoad + rightAxleLoad) / 2.0;
        double axleLoadScore = (avgAxleLoad > properties.getAxleLoadThreshold())
                ? Math.min((avgAxleLoad - properties.getAxleLoadThreshold()) / 100.0, 1.0)
                : 0.0;

        double riskScore = (properties.getWeightSpeed() * speedScore) +
                (properties.getWeightAoa() * aoaScore) +
                (properties.getWeightVibration() * vibrationScore) +
                (properties.getWeightVerticalForce() * verticalForceScore) +
                (properties.getWeightLateralForce() * lateralForceScore) +
                (properties.getWeightLateralVibration() * lateralVibrationScore) +
                (properties.getWeightAxleLoad() * axleLoadScore);

        // Round risk score to one decimal place.
        return Math.round(riskScore * 10.0) / 10.0;
    }
}
