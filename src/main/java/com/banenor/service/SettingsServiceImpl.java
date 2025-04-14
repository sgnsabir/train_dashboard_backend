package com.banenor.service;

import com.banenor.model.UserSettings;
import com.banenor.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private final UserSettingsRepository settingsRepository;

    @Override
    public Mono<UserSettings> getUserSettings(Long userId) {
        log.debug("Fetching settings for userId: {}", userId);
        return settingsRepository.findById(userId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No settings found for userId {}. Creating default settings.", userId);
                    UserSettings defaultSettings = UserSettings.builder()
                            .userId(userId)
                            .username("")
                            .email("")
                            .avatarUrl("/images/default-avatar.png")
                            .showSpeedWidget(true)
                            .showFuelWidget(true)
                            .showPerformanceWidget(true)
                            .enableNotifications(true)
                            .emailAlerts(true)
                            .smsAlerts(false)
                            .twoFactorEnabled(false)
                            .phoneNumber("")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return settingsRepository.save(defaultSettings);
                }));
    }

    @Override
    public Mono<UserSettings> updateUserSettings(Long userId, UserSettings settings) {
        log.debug("Updating settings for userId {}: {}", userId, settings);
        return settingsRepository.findById(userId)
                .flatMap(existing -> {
                    existing.setUsername(settings.getUsername());
                    existing.setEmail(settings.getEmail());
                    existing.setAvatarUrl(settings.getAvatarUrl());
                    existing.setShowSpeedWidget(settings.getShowSpeedWidget());
                    existing.setShowFuelWidget(settings.getShowFuelWidget());
                    existing.setShowPerformanceWidget(settings.getShowPerformanceWidget());
                    existing.setEnableNotifications(settings.getEnableNotifications());
                    existing.setEmailAlerts(settings.getEmailAlerts());
                    existing.setSmsAlerts(settings.getSmsAlerts());
                    existing.setTwoFactorEnabled(settings.getTwoFactorEnabled());
                    existing.setPhoneNumber(settings.getPhoneNumber());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return settingsRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    settings.setUserId(userId);
                    settings.setCreatedAt(LocalDateTime.now());
                    settings.setUpdatedAt(LocalDateTime.now());
                    return settingsRepository.save(settings);
                }));
    }
}
