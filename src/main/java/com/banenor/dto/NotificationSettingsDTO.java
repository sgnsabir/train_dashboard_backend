package com.banenor.dto;

import lombok.Builder;
import lombok.Value;

/**
 * User’s notification preferences.
 */
@Value
@Builder
public class NotificationSettingsDTO {

    /**
     * Master switch for notifications.
     */
    boolean enableNotifications;

    /**
     * Should we send email alerts?
     */
    boolean emailAlerts;

    /**
     * Should we send SMS alerts?
     */
    boolean smsAlerts;
}
