package com.banenor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;


/**
 * Aggregated user settings payload.
 */
@Value
@Builder
public class UserSettingsDTO {

    /**
     * General profile settings (username, email, avatar).
     */
    @Valid
    @NotNull
    GeneralSettingsDTO general;

    /**
     * Which dashboard widgets to show.
     */
    @Valid
    @NotNull
    DashboardSettingsDTO dashboard;

    /**
     * Notification preferences.
     */
    @Valid
    @NotNull
    NotificationSettingsDTO notification;

    /**
     * Security‐related toggles (2FA, phone).
     */
    @Valid
    @NotNull
    SecuritySettingsDTO security;
}
