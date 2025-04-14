package com.banenor.dto;

import lombok.Data;
import java.util.List;

@Data
public class HistoricalDataResponse {
    private Integer analysisId;
    private List<SensorMetricsDTO> metricsHistory;
    private Integer page;
    private Integer size;
    private Long totalRecords;
    private String startTime;
    private String endTime;
}
