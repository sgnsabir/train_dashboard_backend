package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatsDTO {
    private long info;
    private long warn;
    private long critical;
    private long total;
    private LocalDateTime from;
    private LocalDateTime to;
}
