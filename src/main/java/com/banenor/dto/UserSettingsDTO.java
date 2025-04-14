package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for User Settings.
 * Contains nested DTOs for general, dashboard, notification, and security settings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsDTO {

    private GeneralSettingsDTO general;
    private DashboardSettingsDTO dashboard;
    private NotificationSettingsDTO notification;
    private SecuritySettingsDTO security;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneralSettingsDTO {
        /**
         * Username of the user.
         */
        private String username;

        /**
         * Email address of the user.
         */
        private String email;

        /**
         * URL to the user's avatar image.
         */
        private String avatarUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardSettingsDTO {
        /**
         * Flag to indicate if the Speed Widget should be displayed.
         */
        private boolean showSpeedWidget;

        /**
         * Flag to indicate if the Fuel Widget should be displayed.
         */
        private boolean showFuelWidget;

        /**
         * Flag to indicate if the Performance Widget should be displayed.
         */
        private boolean showPerformanceWidget;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationSettingsDTO {
        /**
         * Enable or disable notifications.
         */
        private boolean enableNotifications;

        /**
         * Enable or disable email alerts.
         */
        private boolean emailAlerts;

        /**
         * Enable or disable SMS alerts.
         */
        private boolean smsAlerts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecuritySettingsDTO {
        /**
         * Flag to indicate if two-factor authentication is enabled.
         */
        private boolean twoFactorEnabled;

        /**
         * Phone number to be used for two-factor authentication.
         */
        private String phoneNumber;
    }
}
