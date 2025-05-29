package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class SensorTpSeriesDTO {
    private Integer              analysisId;   // null ⇒ all trains
    private String               sensor;
    private List<Integer>        tp;           // [1,2,3,5,6,8]
    private Map<Integer,List<Double>> values;  // tp → raw readings
}
