package com.banenor.service;

import com.banenor.dto.DashboardSettingsDTO;
import reactor.core.publisher.Mono;

public interface SettingsService {

    /**
     * Fetch the dashboard & notification settings for a given user.
     */
    Mono<DashboardSettingsDTO> getUserSettings(Long userId);

    /**
     * Update the dashboard & notification settings for a given user.
     */
    Mono<DashboardSettingsDTO> updateUserSettings(Long userId, DashboardSettingsDTO settings);
}
