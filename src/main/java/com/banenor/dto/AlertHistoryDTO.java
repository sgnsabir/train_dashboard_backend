package com.banenor.dto;

import lombok.Data;
import java.time.LocalDateTime;
import com.banenor.dto.AlertDTO.Severity;

@Data
public class AlertHistoryDTO {
    private Long id;
    private String subject;
    private String text;
    private LocalDateTime timestamp;
    private Boolean acknowledged;
    private String acknowledgedBy;
    private Integer trainNo;
    private Severity severity;
}
