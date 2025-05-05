package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing a user’s dashboard & notification settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSettingsDTO {
    private Long userId;
    private String username;
    private String email;
    private String avatarUrl;

    // Dashboard widget toggles
    private Boolean showSpeedWidget;
    private Boolean showFuelWidget;
    private Boolean showPerformanceWidget;

    // Notification settings
    private Boolean enableNotifications;
    private Boolean emailAlerts;
    private Boolean smsAlerts;
    private Boolean twoFactorEnabled;

    private String phoneNumber;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
