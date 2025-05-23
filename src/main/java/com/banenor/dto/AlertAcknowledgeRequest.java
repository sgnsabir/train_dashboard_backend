// src/main/java/com/banenor/dto/AlertAcknowledgeRequest.java
package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertAcknowledgeRequest {
    private Long alertId;
    private String acknowledgedBy;
}
