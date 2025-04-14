package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a maintenance schedule record.
 * This DTO encapsulates the maintenance task details including:
 *     timestamp: The complete ISO 8601 formatted timestamp of the task.
 *     id: Unique identifier for the maintenance record.
 *     date: The scheduled date for the maintenance task (formatted as yyyy-MM-dd).
 *     description: A brief description of the maintenance activity.
 * This DTO is intended for use in production-ready endpoints to deliver upcoming maintenance schedules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceScheduleDTO {
    private String timestamp;
    private Integer id;
    private String date;
    private String description;
}
