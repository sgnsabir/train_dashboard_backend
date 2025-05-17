package com.banenor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;

/**
 * Request DTO for triggering a realtime alert.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ParameterObject
public class RealtimeAlertRequest {

    @Min(value = 1, message = "analysisId must be at least 1")
    @NotNull(message = "analysisId is required")
    private Integer analysisId;

    @Email(message = "Invalid email format")
    @NotNull(message = "alertEmail is required")
    private String alertEmail;
}
