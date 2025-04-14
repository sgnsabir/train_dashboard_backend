package com.banenor.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO for raw sensor data responses.
 * Field names are aligned with the model layer.
 */
@Data
public class RawDataResponse {
    private Integer analysisId;
    private LocalDateTime createdAt;
    private Double spdTp1;
    private Double vfrclTp1;
    private Double vfrcrTp1;
    private Double aoaTp1;
    private Double vviblTp1;
    private Double vvibrTp1;
    private Double dtlTp1;
    private Double dtrTp1;
    private Double lfrclTp1;
    private Double lfrcrTp1;
    private Double lviblTp1;
    private Double lvibrTp1;
    // Optionally include longitudinal measurements
    private Double lnglTp1;
    private Double lngrTp1;

    // Fields for sensor filtering
    private String sensorType;
    private Double value;
}
