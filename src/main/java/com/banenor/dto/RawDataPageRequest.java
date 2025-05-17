package com.banenor.dto;

import jakarta.validation.constraints.Min;
import org.springdoc.api.annotations.ParameterObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encapsulates query parameters for paging & filtering raw sensor data endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ParameterObject
public class RawDataPageRequest {

    /**
     * Optional sensor type filter (e.g. "spd", "aoa").
     */
    private String sensorType;

    /**
     * Zero-based page index.
     */
    @Min(value = 0, message = "page must be >= 0")
    private int page = 0;

    /**
     * Page size (number of records per page).
     */
    @Min(value = 1, message = "size must be >= 1")
    private int size = 20;
}
