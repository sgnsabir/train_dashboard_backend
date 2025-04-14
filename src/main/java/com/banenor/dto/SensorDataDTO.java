package com.banenor.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SensorDataDTO {
    @NotBlank(message = "Measurement station must not be blank")
    private String mstation;      // updated from mStation

    @NotBlank(message = "Measurement place must not be blank")
    private String mplace;        // updated from mPlace

    @NotNull(message = "Sensor measurements list must not be null")
    private List<SensorMeasurementDTO> measurements;
    private String sensorId;
    private Integer trainNo;
    private Double speed;
    private Double vibrationLeft;
    private Double vibrationRight;
    private Double verticalForceLeft;
    private Double verticalForceRight;
    private Double lateralForceRight;
    private Double lateralVibrationRight;
    private Long timestamp;
}
