package com.banenor.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

@Table("user_dashboard_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDashboardSettings {

    @Id
    @Column("settings_id")
    private Long settingsId;

    @Column("user_id")
    private Long userId;

    @Column("dashboard_type")
    private String dashboardType;

    @Column("settings")
    private String settings;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
