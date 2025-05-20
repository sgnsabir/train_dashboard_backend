package com.banenor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for alerts.
 * Includes full metadata needed for dashboard visualizations and audit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertDTO {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("subject")
    private String subject;
    @JsonProperty("message")
    private String message;
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    @JsonProperty("severity")
    private Severity severity;
    @JsonProperty("trainNo")
    private Integer trainNo;
    @JsonProperty("acknowledgedBy")
    private String acknowledgedBy;
    @JsonProperty("acknowledged")
    private Boolean acknowledged;
    public enum Severity {
        INFO,
        WARN,
        CRITICAL
    }
}
