package com.banenor.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO for raw sensor data responses.
 * Fields correspond exactly to the available Tp columns in AbstractAxles.
 */
@Data
public class RawDataResponse {
    private Integer analysisId;
    private LocalDateTime createdAt;

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
}
