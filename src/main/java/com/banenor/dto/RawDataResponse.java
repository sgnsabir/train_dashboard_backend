package com.banenor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * DTO carrying one per-TP sensor reading for the frontend.
 * Includes raw Tp measurements plus convenience getters for averages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class RawDataResponse {
    /**
     * The train/analysis identifier.
     */
    @JsonProperty("trainNo")
    private Integer trainNo;

    /**
     * The timestamp of this measurement.
     */
    @JsonProperty("measurementTime")
    @NotNull(message = "Timestamp cannot be null")
    private LocalDateTime measurementTime;

    // --- Raw TP Measurements ---

    // --- Header / Meta fields ---
    private Integer axleId;
    private String ait;
    private String vty;
    private String vit;
    private String aiv;
    private String fe;
    private String idRf2R;

    private Integer analysisId;
    private LocalDateTime createdAt;
    private Integer segmentId;

    // --- Speed Measurements ---
    private Double spdTp1;
    private Double spdTp2;
    private Double spdTp3;
    private Double spdTp5;
    private Double spdTp6;
    private Double spdTp8;

    // --- Vertical Forces (Left) ---
    private Double vfrclTp1;
    private Double vfrclTp2;
    private Double vfrclTp3;
    private Double vfrclTp5;
    private Double vfrclTp6;
    private Double vfrclTp8;

    // --- Vertical Forces (Right) ---
    private Double vfrcrTp1;
    private Double vfrcrTp2;
    private Double vfrcrTp3;
    private Double vfrcrTp5;
    private Double vfrcrTp6;
    private Double vfrcrTp8;

    // --- Angle of Attack ---
    private Double aoaTp1;
    private Double aoaTp2;
    private Double aoaTp3;
    private Double aoaTp5;
    private Double aoaTp6;
    private Double aoaTp8;

    // --- Vertical Vibration (Left) ---
    private Double vviblTp1;
    private Double vviblTp2;
    private Double vviblTp3;
    private Double vviblTp5;
    private Double vviblTp6;
    private Double vviblTp8;

    // --- Vertical Vibration (Right) ---
    private Double vvibrTp1;
    private Double vvibrTp2;
    private Double vvibrTp3;
    private Double vvibrTp5;
    private Double vvibrTp6;
    private Double vvibrTp8;

    // --- Time Delay (Left) ---
    private Double dtlTp1;
    private Double dtlTp2;
    private Double dtlTp3;
    private Double dtlTp5;
    private Double dtlTp6;
    private Double dtlTp8;

    // --- Time Delay (Right) ---
    private Double dtrTp1;
    private Double dtrTp2;
    private Double dtrTp3;
    private Double dtrTp5;
    private Double dtrTp6;
    private Double dtrTp8;

    // --- Lateral Force (Left) ---
    private Double lfrclTp1;
    private Double lfrclTp2;
    private Double lfrclTp3;
    private Double lfrclTp5;
    private Double lfrclTp6;

    // --- Lateral Force (Right) ---
    private Double lfrcrTp1;
    private Double lfrcrTp2;
    private Double lfrcrTp3;
    private Double lfrcrTp5;
    private Double lfrcrTp6;

    // --- Lateral Vibration (Left) ---
    private Double lviblTp1;
    private Double lviblTp2;
    private Double lviblTp3;
    private Double lviblTp5;
    private Double lviblTp6;

    // --- Lateral Vibration (Right) ---
    private Double lvibrTp1;
    private Double lvibrTp2;
    private Double lvibrTp3;
    private Double lvibrTp5;
    private Double lvibrTp6;

    // --- Longitudinal Measurements ---
    private Double lnglTp1;
    private Double lnglTp8;
    private Double lngrTp1;
    private Double lngrTp8;

    // For filtering by a specific sensor type at runtime
    private String sensorType;
    private Double value;

    // --- Convenience Getters for Dashboard Charts ---

    private static Double avg(Double... vals) {
        double sum = 0;
        int count = 0;
        for (Double v : vals) {
            if (v != null) {
                sum += v;
                count++;
            }
        }
        return count > 0 ? sum / count : null;
    }

    @JsonProperty("averageSpeed")
    public Double getAverageSpeed() {
        return avg(spdTp1, spdTp2, spdTp3, spdTp5, spdTp6, spdTp8);
    }

    @JsonProperty("averageAoa")
    public Double getAverageAoa() {
        return avg(aoaTp1, aoaTp2, aoaTp3, aoaTp5, aoaTp6, aoaTp8);
    }

    @JsonProperty("averageVerticalForceLeft")
    public Double getAverageVerticalForceLeft() {
        return avg(vfrclTp1, vfrclTp2, vfrclTp3, vfrclTp5, vfrclTp6, vfrclTp8);
    }

    @JsonProperty("averageVerticalForceRight")
    public Double getAverageVerticalForceRight() {
        return avg(vfrcrTp1, vfrcrTp2, vfrcrTp3, vfrcrTp5, vfrcrTp6, vfrcrTp8);
    }

    @JsonProperty("averageVibrationLeft")
    public Double getAverageVerticalVibrationLeft() {
        return avg(vviblTp1, vviblTp2, vviblTp3, vviblTp5, vviblTp6, vviblTp8);
    }

    @JsonProperty("averageVibrationRight")
    public Double getAverageVerticalVibrationRight() {
        return avg(vvibrTp1, vvibrTp2, vvibrTp3, vvibrTp5, vvibrTp6, vvibrTp8);
    }

    @JsonProperty("averageLateralForceLeft")
    public Double getAverageLateralForceLeft() {
        return avg(lfrclTp1, lfrclTp2, lfrclTp3, lfrclTp5, lfrclTp6);
    }

    @JsonProperty("averageLateralForceRight")
    public Double getAverageLateralForceRight() {
        return avg(lfrcrTp1, lfrcrTp2, lfrcrTp3, lfrcrTp5, lfrcrTp6);
    }

    @JsonProperty("averageLateralVibrationLeft")
    public Double getAverageLateralVibrationLeft() {
        return avg(lviblTp1, lviblTp2, lviblTp3, lviblTp5, lviblTp6);
    }

    @JsonProperty("averageLateralVibrationRight")
    public Double getAverageLateralVibrationRight() {
        return avg(lvibrTp1, lvibrTp2, lvibrTp3, lvibrTp5, lvibrTp6);
    }

    @JsonProperty("averageTimeDelayLeft")
    public Double getAverageTimeDelayLeft() {
        return avg(dtlTp1, dtlTp2, dtlTp3, dtlTp5, dtlTp6, dtlTp8);
    }

    @JsonProperty("averageTimeDelayRight")
    public Double getAverageTimeDelayRight() {
        return avg(dtrTp1, dtrTp2, dtrTp3, dtrTp5, dtrTp6, dtrTp8);
    }

    @JsonProperty("averageLongitudinal")
    public Double getAverageLongitudinal() {
        Double left = avg(lnglTp1, lnglTp8);
        Double right = avg(lngrTp1, lngrTp8);
        if (left != null && right != null) return (left + right) / 2.0;
        return left != null ? left : right;
    }

    @JsonProperty("averageLongitudinalLeft")
    public Double getAverageLongitudinalLeft() {
        return avg(lnglTp1, lnglTp8);
    }

    @JsonProperty("averageLongitudinalRight")
    public Double getAverageLongitudinalRight() {
        return avg(lngrTp1, lngrTp8);
    }

    @JsonProperty("overallAverageLongitudinal")
    public Double getOverallAverageLongitudinal() {
        Double left = getAverageLongitudinalLeft();
        Double right = getAverageLongitudinalRight();
        if (left != null && right != null) return (left + right) / 2.0;
        return left != null ? left : right;
    }

}
