package com.banenor.dto;

import lombok.Data;
import java.util.List;

@Data
public class AnalysisFileDTO {
    private AnalysisHeaderDTO header;
    private List<?> axles;
}
