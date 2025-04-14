package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorMeasurementDTO {

    // Speed Measurements
    private Double spdTp1;
    private Double spdTp2;
    private Double spdTp3;
    private Double spdTp5;
    private Double spdTp6;
    private Double spdTp8;

    // Angle of Attack (AOA) Measurements
    private Double aoaTp1;
    private Double aoaTp2;
    private Double aoaTp3;
    private Double aoaTp5;
    private Double aoaTp6;
    private Double aoaTp8;

    // Vertical Force Measurements - Left
    private Double vfrclTp1;
    private Double vfrclTp2;
    private Double vfrclTp3;
    private Double vfrclTp5;
    private Double vfrclTp6;
    private Double vfrclTp8;

    // Vertical Force Measurements - Right
    private Double vfrcrTp1;
    private Double vfrcrTp2;
    private Double vfrcrTp3;
    private Double vfrcrTp5;
    private Double vfrcrTp6;
    private Double vfrcrTp8;

    // Vertical Vibration Measurements - Left
    private Double vviblTp1;
    private Double vviblTp2;
    private Double vviblTp3;
    private Double vviblTp5;
    private Double vviblTp6;
    private Double vviblTp8;

    // Vertical Vibration Measurements - Right
    private Double vvibrTp1;
    private Double vvibrTp2;
    private Double vvibrTp3;
    private Double vvibrTp5;
    private Double vvibrTp6;
    private Double vvibrTp8;

    // Time Delay Measurements - Left
    private Double dtlTp1;
    private Double dtlTp2;
    private Double dtlTp3;
    private Double dtlTp5;
    private Double dtlTp6;
    private Double dtlTp8;

    // Time Delay Measurements - Right
    private Double dtrTp1;
    private Double dtrTp2;
    private Double dtrTp3;
    private Double dtrTp5;
    private Double dtrTp6;
    private Double dtrTp8;

    // Lateral Force Measurements - Left
    private Double lfrclTp1;
    private Double lfrclTp2;
    private Double lfrclTp3;
    private Double lfrclTp5;
    private Double lfrclTp6;

    // Lateral Force Measurements - Right
    private Double lfrcrTp1;
    private Double lfrcrTp2;
    private Double lfrcrTp3;
    private Double lfrcrTp5;
    private Double lfrcrTp6;

    // Lateral Vibration Measurements - Left
    private Double lviblTp1;
    private Double lviblTp2;
    private Double lviblTp3;
    private Double lviblTp5;
    private Double lviblTp6;

    // Lateral Vibration Measurements - Right
    private Double lvibrTp1;
    private Double lvibrTp2;
    private Double lvibrTp3;
    private Double lvibrTp5;
    private Double lvibrTp6;

    // Longitudinal Measurements
    private Double lnglTp1;
    private Double lnglTp8;
    private Double lngrTp1;
    private Double lngrTp8;

    // --- Helper Methods for Aggregation ---

    /**
     * Computes the average of provided Double values while ignoring nulls.
     *
     * @param values one or more Double values.
     * @return the average of non-null values, or null if none are provided.
     */
    private static Double average(Double... values) {
        double sum = 0.0;
        int count = 0;
        for (Double value : values) {
            if (value != null) {
                sum += value;
                count++;
            }
        }
        return count > 0 ? sum / count : null;
    }

    /**
     * Computes the overall average speed from all available speed measurements.
     *
     * @return average speed, or null if none are provided.
     */
    public Double getAverageSpeed() {
        return average(spdTp1, spdTp2, spdTp3, spdTp5, spdTp6, spdTp8);
    }

    /**
     * Computes the overall average angle of attack (AOA).
     *
     * @return average AOA, or null if none are provided.
     */
    public Double getAverageAoa() {
        return average(aoaTp1, aoaTp2, aoaTp3, aoaTp5, aoaTp6, aoaTp8);
    }

    /**
     * Computes the average vertical force for the left side.
     *
     * @return average vertical force (left), or null if none are provided.
     */
    public Double getAverageVerticalForceLeft() {
        return average(vfrclTp1, vfrclTp2, vfrclTp3, vfrclTp5, vfrclTp6, vfrclTp8);
    }

    /**
     * Computes the average vertical force for the right side.
     *
     * @return average vertical force (right), or null if none are provided.
     */
    public Double getAverageVerticalForceRight() {
        return average(vfrcrTp1, vfrcrTp2, vfrcrTp3, vfrcrTp5, vfrcrTp6, vfrcrTp8);
    }

    /**
     * Computes the average vertical vibration for the left side.
     *
     * @return average vertical vibration (left), or null if none are provided.
     */
    public Double getAverageVerticalVibrationLeft() {
        return average(vviblTp1, vviblTp2, vviblTp3, vviblTp5, vviblTp6, vviblTp8);
    }

    /**
     * Computes the average vertical vibration for the right side.
     *
     * @return average vertical vibration (right), or null if none are provided.
     */
    public Double getAverageVerticalVibrationRight() {
        return average(vvibrTp1, vvibrTp2, vvibrTp3, vvibrTp5, vvibrTp6, vvibrTp8);
    }

    /**
     * Computes the average time delay for the left side.
     *
     * @return average time delay (left), or null if none are provided.
     */
    public Double getAverageTimeDelayLeft() {
        return average(dtlTp1, dtlTp2, dtlTp3, dtlTp5, dtlTp6, dtlTp8);
    }

    /**
     * Computes the average time delay for the right side.
     *
     * @return average time delay (right), or null if none are provided.
     */
    public Double getAverageTimeDelayRight() {
        return average(dtrTp1, dtrTp2, dtrTp3, dtrTp5, dtrTp6, dtrTp8);
    }

    /**
     * Computes the average lateral force for the left side.
     *
     * @return average lateral force (left), or null if none are provided.
     */
    public Double getAverageLateralForceLeft() {
        return average(lfrclTp1, lfrclTp2, lfrclTp3, lfrclTp5, lfrclTp6);
    }

    /**
     * Computes the average lateral force for the right side.
     *
     * @return average lateral force (right), or null if none are provided.
     */
    public Double getAverageLateralForceRight() {
        return average(lfrcrTp1, lfrcrTp2, lfrcrTp3, lfrcrTp5, lfrcrTp6);
    }

    /**
     * Computes the average lateral vibration for the left side.
     *
     * @return average lateral vibration (left), or null if none are provided.
     */
    public Double getAverageLateralVibrationLeft() {
        return average(lviblTp1, lviblTp2, lviblTp3, lviblTp5, lviblTp6);
    }

    /**
     * Computes the average lateral vibration for the right side.
     *
     * @return average lateral vibration (right), or null if none are provided.
     */
    public Double getAverageLateralVibrationRight() {
        return average(lvibrTp1, lvibrTp2, lvibrTp3, lvibrTp5, lvibrTp6);
    }

    /**
     * Computes the average longitudinal measurement for the left side.
     *
     * @return average longitudinal measurement (left), or null if none are provided.
     */
    public Double getAverageLongitudinalLeft() {
        return average(lnglTp1, lnglTp8);
    }

    /**
     * Computes the average longitudinal measurement for the right side.
     *
     * @return average longitudinal measurement (right), or null if none are provided.
     */
    public Double getAverageLongitudinalRight() {
        return average(lngrTp1, lngrTp8);
    }

    /**
     * Computes the overall average longitudinal measurement by combining left and right averages.
     *
     * @return overall average longitudinal measurement, or null if no valid values.
     */
    public Double getOverallAverageLongitudinal() {
        Double left = getAverageLongitudinalLeft();
        Double right = getAverageLongitudinalRight();
        if (left != null && right != null) {
            return (left + right) / 2.0;
        } else if (left != null) {
            return left;
        } else {
            return right;
        }
    }
}
