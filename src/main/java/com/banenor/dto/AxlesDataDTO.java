package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AxlesDataDTO {
    private Integer trainNo;
    private String measurementPoint;
    private LocalDateTime createdAt;
    private Double speed;
    private Double angleOfAttack;
    private Double vibrationLeft;
    private Double vibrationRight;
    private Double verticalForceLeft;
    private Double verticalForceRight;
    private Double lateralForceLeft;
    private Double lateralForceRight;
    private Double lateralVibrationLeft;
    private Double lateralVibrationRight;
}
