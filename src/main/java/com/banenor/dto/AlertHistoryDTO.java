package com.banenor.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AlertHistoryDTO {
    private Long id;
    private String subject;
    private String text;
    private LocalDateTime timestamp;
    private Boolean acknowledged;
}
