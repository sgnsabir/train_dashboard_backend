package com.banenor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("user_settings")
public class UserSettings {

    @Id
    private Long userId; // primary key (equals the user id)

    // General Settings
    private String username;
    private String email;
    private String avatarUrl;

    // Dashboard Widget Settings
    private Boolean showSpeedWidget;
    private Boolean showFuelWidget;
    private Boolean showPerformanceWidget;

    // Notification Settings
    private Boolean enableNotifications;
    private Boolean emailAlerts;
    private Boolean smsAlerts;

    // Security Settings
    private Boolean twoFactorEnabled;
    private String phoneNumber;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;
    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
