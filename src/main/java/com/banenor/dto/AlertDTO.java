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

    /**
     * Unique identifier for the alert.
     */
    @JsonProperty("id")
    private Long id;

    /**
     * title of the alert.
     */
    @JsonProperty("subject")
    private String subject;

    /**
     * Descriptive message body of the alert.
     */
    @JsonProperty("message")
    private String message;

    /**
     * When the alert was generated.
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * Severity classification for filtering and visualization.
     */
    @JsonProperty("severity")
    private Severity severity;

    /**
     * The train number associated with this alert.
     */
    @JsonProperty("trainNo")
    private Integer trainNo;

    /**
     * Username or system component that acknowledged the alert.
     */
    @JsonProperty("acknowledgedBy")
    private String acknowledgedBy;

    /**
     * Whether the alert has been acknowledged.
     * Kept for backward compatibility with existing clients.
     */
    @JsonProperty("acknowledged")
    private Boolean acknowledged;

    /**
     * Enumeration of possible alert severities.
     */
    public enum Severity {
        INFO,
        WARN,
        CRITICAL
    }
}
