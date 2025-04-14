package com.banenor.dto;

import lombok.Data;

@Data
public class SensorDataPayload {
    private AnalysisHeaderDTO header;
    private AnalysisMeasurementDTO measurement;
}
