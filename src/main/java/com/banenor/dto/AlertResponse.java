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
    private Long id;
    private String subject;
    @JsonProperty("message")
    private String message;
    private LocalDateTime timestamp;
    private Boolean acknowledged;
    private String acknowledgedBy;
    private Integer trainNo;
    private Severity severity;
}
