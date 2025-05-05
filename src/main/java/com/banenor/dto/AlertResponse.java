package com.banenor.dto;

import com.banenor.dto.AlertDTO.Severity;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertResponse {

    /**
     * Unique identifier for the alert.
     */
    private Long id;

    /**
     * Title of the alert.
     */
    private String subject;

    /**
     * Descriptive message body of the alert.
     */
    @JsonProperty("message")
    private String message;

    /**
     * When the alert was generated.
     */
    private LocalDateTime timestamp;

    /**
     * Whether the alert has been acknowledged.
     * Changed to Boolean so Lombok will generate getAcknowledged().
     */
    private Boolean acknowledged;

    /**
     * Username or component that acknowledged the alert.
     */
    private String acknowledgedBy;

    /**
     * The train number associated with this alert.
     */
    private Integer trainNo;

    /**
     * Severity classification for filtering and visualization.
     */
    private Severity severity;
}
