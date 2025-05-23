package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceScheduleDTO {
    private String timestamp;
    private Integer id;
    private String dueDate;
    private String task;
    private String asset;
    private String status;
}
