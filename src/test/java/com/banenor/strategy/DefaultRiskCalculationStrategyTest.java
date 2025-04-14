package com.banenor.strategy;

import com.banenor.config.MaintenanceProperties;
import com.banenor.dto.SensorMetricsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.*;

@TestPropertySource(locations = "classpath:test.properties")
class DefaultRiskCalculationStrategyTest {

    private DefaultRiskCalculationStrategy strategy;
    private MaintenanceProperties properties;

    @BeforeEach
    void setUp() {
        strategy = new DefaultRiskCalculationStrategy();
        properties = new MaintenanceProperties();
        // Set thresholds for sensor metrics
        properties.setSpeedThreshold(80.0);
        properties.setAoaThreshold(5.0);
        properties.setVibrationThreshold(3.0);
        properties.setVerticalForceThreshold(500.0);
        properties.setLateralForceThreshold(300.0);
        properties.setLateralVibrationThreshold(2.0);
        properties.setAxleLoadThreshold(1000.0);

        // Set weights for each sensor metric (weights need not sum to 1)
        properties.setWeightSpeed(0.20);
        properties.setWeightAoa(0.10);
        properties.setWeightVibration(0.10);
        properties.setWeightVerticalForce(0.10);
        properties.setWeightLateralForce(0.10);
        properties.setWeightLateralVibration(0.10);
        properties.setWeightAxleLoad(0.10);

        properties.setRiskScoreThreshold(0.7);
    }

    @Test
    void testCalculateRisk_AllBelowThresholds() {
        SensorMetricsDTO metrics = new SensorMetricsDTO();
        metrics.setAverageSpeed(70.0);
        metrics.setAverageAoa(4.0);
        metrics.setAverageVibrationLeft(2.0);
        metrics.setAverageVibrationRight(2.0);
        metrics.setAverageVerticalForceLeft(400.0);
        metrics.setAverageVerticalForceRight(400.0);
        metrics.setAverageLateralForceLeft(200.0);
        metrics.setAverageLateralForceRight(200.0);
        metrics.setAverageAxleLoadLeft(900.0);
        // Explicitly set lateral vibration values equal to threshold (2.0)
        metrics.setAverageLateralVibrationLeft(2.0);
        metrics.setAverageLateralVibrationRight(2.0);

        double riskScore = strategy.calculateRisk(metrics, properties);
        assertThat(riskScore)
                .as("When all sensor values are below thresholds, risk should be 0")
                .isEqualTo(0.0);
    }

    @Test
    void testCalculateRisk_AllAtThresholds() {
        SensorMetricsDTO metrics = new SensorMetricsDTO();
        metrics.setAverageSpeed(80.0);
        metrics.setAverageAoa(5.0);
        metrics.setAverageVibrationLeft(3.0);
        metrics.setAverageVibrationRight(3.0);
        metrics.setAverageVerticalForceLeft(500.0);
        metrics.setAverageVerticalForceRight(500.0);
        metrics.setAverageLateralForceLeft(300.0);
        metrics.setAverageLateralForceRight(300.0);
        metrics.setAverageAxleLoadLeft(1000.0);
        // Set lateral vibration exactly at its threshold so score = 0
        metrics.setAverageLateralVibrationLeft(2.0);
        metrics.setAverageLateralVibrationRight(2.0);

        double riskScore = strategy.calculateRisk(metrics, properties);
        assertThat(riskScore)
                .as("When sensor values equal thresholds, risk should be 0")
                .isEqualTo(0.0);
    }

    @Test
    void testCalculateRisk_SomeExceedThresholds() {
        SensorMetricsDTO metrics = new SensorMetricsDTO();
        metrics.setAverageSpeed(90.0);       // 10 above threshold; score = 0.5 → contribution = 0.10
        metrics.setAverageAoa(7.0);          // 2 above threshold; score = 0.4 → contribution = 0.04
        metrics.setAverageVibrationLeft(4.0);
        metrics.setAverageVibrationRight(4.0); // average = 4.0; score = 0.5 → 0.05
        metrics.setAverageVerticalForceLeft(600.0);
        metrics.setAverageVerticalForceRight(600.0); // score = 1 → 0.10
        metrics.setAverageLateralForceLeft(350.0);
        metrics.setAverageLateralForceRight(350.0); // score = 1 → 0.10
        metrics.setAverageAxleLoadLeft(1100.0);    // score = 1 → 0.10
        // Set lateral vibration values moderately above threshold (e.g., 3.0 so score = 1.0 is not reached)
        metrics.setAverageLateralVibrationLeft(3.0);
        metrics.setAverageLateralVibrationRight(3.0);

        double riskScore = strategy.calculateRisk(metrics, properties);
        assertThat(riskScore)
                .as("Risk score should be positive when some values exceed thresholds")
                .isGreaterThan(0.0)
                .isLessThan(1.0);
    }

    @Test
    void testCalculateRisk_NullMetrics() {
        assertThatThrownBy(() -> strategy.calculateRisk(null, properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sensor metrics cannot be null");
    }

    @Test
    void testCalculateRisk_MissingRightVibration() {
        SensorMetricsDTO metrics = new SensorMetricsDTO();
        metrics.setAverageSpeed(90.0);
        metrics.setAverageAoa(7.0);
        metrics.setAverageVibrationLeft(4.0);
        // Do not set averageVibrationRight; assume left value is used.
        metrics.setAverageVerticalForceLeft(600.0);
        metrics.setAverageVerticalForceRight(600.0);
        metrics.setAverageLateralForceLeft(350.0);
        metrics.setAverageLateralForceRight(350.0);
        metrics.setAverageAxleLoadLeft(1100.0);
        // Set lateral vibration only on left side
        metrics.setAverageLateralVibrationLeft(3.0);
        // averageLateralVibrationRight is missing
        double riskScore = strategy.calculateRisk(metrics, properties);
        assertThat(riskScore)
                .as("Risk score should be computed correctly even if one vibration side is missing")
                .isGreaterThan(0.0);
    }

    @Test
    void testCalculateRisk_NegativeValues() {
        SensorMetricsDTO metrics = new SensorMetricsDTO();
        metrics.setAverageSpeed(-10.0);
        metrics.setAverageAoa(-5.0);
        metrics.setAverageVibrationLeft(-1.0);
        metrics.setAverageVibrationRight(-1.0);
        metrics.setAverageVerticalForceLeft(-100.0);
        metrics.setAverageVerticalForceRight(-100.0);
        metrics.setAverageLateralForceLeft(-50.0);
        metrics.setAverageLateralForceRight(-50.0);
        metrics.setAverageAxleLoadLeft(-500.0);
        // Set lateral vibration explicitly to 0
        metrics.setAverageLateralVibrationLeft(0.0);
        metrics.setAverageLateralVibrationRight(0.0);

        double riskScore = strategy.calculateRisk(metrics, properties);
        assertThat(riskScore)
                .as("Negative sensor values should result in a risk score of 0")
                .isEqualTo(0.0);
    }

    @Test
    void testCalculateRisk_ExtremeValues() {
        SensorMetricsDTO metrics = new SensorMetricsDTO();
        metrics.setAverageSpeed(300.0);   // score capped at 1 → 0.20
        metrics.setAverageAoa(20.0);      // score capped at 1 → 0.10
        metrics.setAverageVibrationLeft(10.0);
        metrics.setAverageVibrationRight(10.0); // score capped at 1 → 0.10
        metrics.setAverageVerticalForceLeft(1500.0);
        metrics.setAverageVerticalForceRight(1500.0); // score capped at 1 → 0.10
        metrics.setAverageLateralForceLeft(800.0);
        metrics.setAverageLateralForceRight(800.0); // score capped at 1 → 0.10
        metrics.setAverageAxleLoadLeft(2500.0);    // score capped at 1 → 0.10
        // Set lateral vibration values to extreme (10.0)
        metrics.setAverageLateralVibrationLeft(10.0);
        metrics.setAverageLateralVibrationRight(10.0);

        double riskScore = strategy.calculateRisk(metrics, properties);
        assertThat(riskScore)
                .as("Extreme values should result in maximum risk score capped by weights")
                .isEqualTo(0.80);
    }
}
