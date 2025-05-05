package com.banenor.repository;

import com.banenor.model.UserDashboardSettings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Custom operations for user dashboard settings.
 */
public interface UserDashboardSettingsRepositoryCustom {

    /**
     * Fetch all dashboard settings for a given user.
     */
    Flux<UserDashboardSettings> findByUserId(Long userId);

    /**
     * Insert or update (upsert) a single setting for a user.
     */
    Mono<Void> upsertSetting(Long userId, String settingKey, String settingValue);

    /**
     * Delete a specific setting for a user.
     */
    Mono<Void> deleteSetting(Long userId, String settingKey);
}
