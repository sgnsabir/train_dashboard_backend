package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemDashboardDTO {
    private SensorMetricsDTO metrics;
    private List<AlertDTO> recentAlerts;
    private String systemStatus;
}
