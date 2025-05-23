package com.banenor.service;

import com.banenor.dto.*;
import reactor.core.publisher.Mono;

public interface SettingsService {

    // General profile settings
    Mono<GeneralSettingsDTO> getGeneralSettings(Long userId);
    Mono<GeneralSettingsDTO> updateGeneralSettings(Long userId, GeneralSettingsDTO settings);

    // Dashboard widget settings
    Mono<DashboardSettingsDTO> getDashboardSettings(Long userId);
    Mono<DashboardSettingsDTO> updateDashboardSettings(Long userId, DashboardSettingsDTO settings);

    // Notification preferences
    Mono<NotificationSettingsDTO> getNotificationSettings(Long userId);
    Mono<NotificationSettingsDTO> updateNotificationSettings(Long userId, NotificationSettingsDTO settings);

    // Security settings (2FA, phone)
    Mono<SecuritySettingsDTO> getSecuritySettings(Long userId);
    Mono<SecuritySettingsDTO> updateSecuritySettings(Long userId, SecuritySettingsDTO settings);
}
