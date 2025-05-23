package com.banenor.dto;

import io.r2dbc.spi.Blob;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "User", description = "Unified user profile and settings DTO")
public class UserDTO {

    @Schema(description = "Unique user identifier", example = "42")
    private Long id;

    @Schema(description = "Username or display name", example = "janne.doe")
    private String username;

    @Schema(description = "Primary email address", example = "janne.doe@banenor.com")
    private String email;

    @Schema(description = "Assigned security roles", example = "[\"ROLE_USER\",\"ROLE_ADMIN\"]")
    private List<String> roles;

    @Schema(description = "URL of avatar image", example = "https://cdn.banenor.com/avatars/42.png")
    private String avatarUrl;

    @Schema(description = "Phone number for SMS/2FA", example = "+1234567890")
    private String phone;

    @Schema(description = "Two-factor authentication enabled", example = "true")
    private Boolean twoFactorEnabled;

    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Record last-updated timestamp")
    private LocalDateTime updatedAt;
}
