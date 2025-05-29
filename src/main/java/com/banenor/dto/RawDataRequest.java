package com.banenor.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;

/**
 * Wrapper for query parameters of GET /api/v1/data/raw/{analysisId}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ParameterObject
public class RawDataRequest {
    @Min(value = 0, message = "page must be >= 0")
    private int page = 0;
    @Min(value = 1, message = "size must be >= 1")
    private int size = 20;
    private String sensorType;
}
