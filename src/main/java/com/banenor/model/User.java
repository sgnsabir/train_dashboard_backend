package com.banenor.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column("user_id")
    private Long userId;

    @Column("username")
    private String username;

    @Column("email")
    private String email;

    @Column("password")
    private String password;

    @Column("role")
    private String role;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    // Newly added fields to support user profile and settings
    @Column("avatar")
    private String avatar;

    @Column("two_factor_enabled")
    private Boolean twoFactorEnabled;

    @Column("phone")
    private String phone;

    @Column("enabled")
    private boolean enabled;

    @Column("locked")
    private boolean locked;

    @Column("last_login_attempt")
    private long lastLoginAttempt;

    @Column("failed_login_attempts")
    private int failedLoginAttempts;
}
