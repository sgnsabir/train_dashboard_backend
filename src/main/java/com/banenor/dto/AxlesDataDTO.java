package com.banenor.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AxlesDataDTO {
    private Integer trainNo;
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
    private LocalDateTime createdAt;
    private String measurementPoint; // "MP1" or "MP3"
} 
