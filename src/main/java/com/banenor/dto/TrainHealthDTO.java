package com.banenor.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for TrainHealth.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainHealthDTO {
    private Integer id;
    private Integer trainNo;
    private Double healthScore;
    private Integer faultCount;
    private LocalDateTime timestamp;
}
