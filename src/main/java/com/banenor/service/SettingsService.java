package com.banenor.service;

import com.banenor.model.UserSettings;
import reactor.core.publisher.Mono;

public interface SettingsService {

    /**
     * Retrieves settings for the given user.
     *
     * @param userId the unique identifier of the user.
     * @return a Mono emitting the UserSettings for the given userId.
     */
    Mono<UserSettings> getUserSettings(Long userId);

    /**
     * Updates settings for the given user.
     *
     * @param userId the unique identifier of the user.
     * @param settings the new settings payload.
     * @return a Mono emitting the updated UserSettings.
     */
    Mono<UserSettings> updateUserSettings(Long userId, UserSettings settings);
}
