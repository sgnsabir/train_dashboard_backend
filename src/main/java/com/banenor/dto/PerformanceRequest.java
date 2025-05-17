// src/main/java/com/banenor/dto/PerformanceRequest.java
package com.banenor.dto;

import org.springframework.format.annotation.DateTimeFormat;
import org.springdoc.api.annotations.ParameterObject;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@ParameterObject
public class PerformanceRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    // Defaulting logic moved here:
    public LocalDateTime getFrom() {
        LocalDateTime now = LocalDateTime.now();
        return (startDate != null ? startDate : now.minusDays(7));
    }

    public LocalDateTime getTo() {
        LocalDateTime now = LocalDateTime.now();
        return (endDate   != null ? endDate   : now);
    }

    // Standard getters/setters
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate()   { return endDate;   }
    public void setEndDate(LocalDateTime endDate)   { this.endDate   = endDate;   }
}
