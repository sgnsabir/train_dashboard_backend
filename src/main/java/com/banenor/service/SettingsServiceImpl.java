package com.banenor.service;

import com.banenor.dto.*;
import com.banenor.model.UserSettings;
import com.banenor.repository.UserSettingsRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private final UserSettingsRepository repo;

    @Override
    public Mono<GeneralSettingsDTO> getGeneralSettings(Long userId) {
        return fetchEntity(userId)
                .map(this::toGeneralDto)
                .doOnSuccess(dto -> log.info("Loaded general settings for userId={}", userId))
                .doOnError(ex -> log.error("Error loading general settings for userId={}", userId, ex));
    }

    @Override
    public Mono<GeneralSettingsDTO> updateGeneralSettings(Long userId, GeneralSettingsDTO dto) {
        return fetchEntity(userId)
                .flatMap(e -> {
                    e.setUsername(dto.getUsername());
                    e.setEmail(dto.getEmail());
                    e.setAvatarUrl(dto.getAvatarUrl());
                    e.setLanguage(dto.getLanguage());
                    e.setTimezone(dto.getTimezone());
                    e.setDateFormat(dto.getDateFormat());
                    e.setTheme(UserSettings.Theme.valueOf(dto.getTheme().name()));
                    e.setUpdatedAt(LocalDateTime.now());
                    return repo.save(e);
                })
                .map(this::toGeneralDto)
                .doOnSuccess(d -> log.info("Updated general settings for userId={}", userId))
                .doOnError(ex -> log.error("Error updating general settings for userId={}", userId, ex));
    }

    @Override
    public Mono<DashboardSettingsDTO> getDashboardSettings(Long userId) {
        return fetchEntity(userId)
                .map(this::toDashboardDto)
                .doOnSuccess(dto -> log.info("Loaded dashboard settings for userId={}", userId))
                .doOnError(ex -> log.error("Error loading dashboard settings for userId={}", userId, ex));
    }

    @Override
    public Mono<DashboardSettingsDTO> updateDashboardSettings(Long userId, DashboardSettingsDTO dto) {
        if (!userId.equals(dto.getUserId())) {
            return Mono.error(new ValidationException("Path userId and body.userId must match"));
        }
        return fetchEntity(userId)
                .flatMap(e -> {
                    e.setShowSpeedWidget(dto.getShowSpeedWidget());
                    e.setShowFuelWidget(dto.getShowFuelWidget());
                    e.setShowPerformanceWidget(dto.getShowPerformanceWidget());
                    e.setUpdatedAt(LocalDateTime.now());
                    return repo.save(e);
                })
                .map(this::toDashboardDto)
                .doOnSuccess(d -> log.info("Updated dashboard settings for userId={}", userId))
                .doOnError(ex -> log.error("Error updating dashboard settings for userId={}", userId, ex));
    }

    @Override
    public Mono<NotificationSettingsDTO> getNotificationSettings(Long userId) {
        return fetchEntity(userId)
                .map(this::toNotificationDto)
                .doOnSuccess(dto -> log.info("Loaded notification settings for userId={}", userId))
                .doOnError(ex -> log.error("Error loading notification settings for userId={}", userId, ex));
    }

    @Override
    public Mono<NotificationSettingsDTO> updateNotificationSettings(Long userId, NotificationSettingsDTO dto) {
        return fetchEntity(userId)
                .flatMap(e -> {
                    e.setEnableNotifications(dto.isEnableNotifications());
                    e.setEmailAlerts(dto.isEmailAlerts());
                    e.setSmsAlerts(dto.isSmsAlerts());
                    e.setUpdatedAt(LocalDateTime.now());
                    return repo.save(e);
                })
                .map(this::toNotificationDto)
                .doOnSuccess(d -> log.info("Updated notification settings for userId={}", userId))
                .doOnError(ex -> log.error("Error updating notification settings for userId={}", userId, ex));
    }

    @Override
    public Mono<SecuritySettingsDTO> getSecuritySettings(Long userId) {
        return fetchEntity(userId)
                .map(this::toSecurityDto)
                .doOnSuccess(dto -> log.info("Loaded security settings for userId={}", userId))
                .doOnError(ex -> log.error("Error loading security settings for userId={}", userId, ex));
    }

    @Override
    public Mono<SecuritySettingsDTO> updateSecuritySettings(Long userId, SecuritySettingsDTO dto) {
        return fetchEntity(userId)
                .flatMap(e -> {
                    e.setTwoFactorEnabled(dto.isTwoFactorEnabled());
                    e.setPhoneNumber(dto.getPhoneNumber());
                    e.setUpdatedAt(LocalDateTime.now());
                    return repo.save(e);
                })
                .map(this::toSecurityDto)
                .doOnSuccess(d -> log.info("Updated security settings for userId={}", userId))
                .doOnError(ex -> log.error("Error updating security settings for userId={}", userId, ex));
    }

    private Mono<UserSettings> fetchEntity(Long userId) {
        return repo.findById(userId)
                .defaultIfEmpty(defaultEntity(userId))
                .onErrorMap(ex -> new IllegalStateException("Error fetching settings for userId=" + userId, ex));
    }

    private UserSettings defaultEntity(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return UserSettings.builder()
                .userId(userId)
                // General defaults
                .username("user" + userId)
                .email("")
                .avatarUrl("")
                .language("en")
                .timezone("UTC")
                .dateFormat("yyyy-MM-dd")
                .theme(UserSettings.Theme.SYSTEM)
                // Dashboard defaults
                .showSpeedWidget(true)
                .showFuelWidget(true)
                .showPerformanceWidget(true)
                // Notification defaults
                .enableNotifications(true)
                .emailAlerts(true)
                .smsAlerts(false)
                // Security defaults
                .twoFactorEnabled(false)
                .phoneNumber("")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private GeneralSettingsDTO toGeneralDto(UserSettings e) {
        return GeneralSettingsDTO.builder()
                .username(e.getUsername())
                .email(e.getEmail())
                .avatarUrl(e.getAvatarUrl())
                .language(e.getLanguage())
                .timezone(e.getTimezone())
                .dateFormat(e.getDateFormat())
                .theme(GeneralSettingsDTO.Theme.valueOf(e.getTheme().name()))
                .build();
    }

    private DashboardSettingsDTO toDashboardDto(UserSettings e) {
        return DashboardSettingsDTO.builder()
                .userId(e.getUserId())
                .username(e.getUsername())
                .email(e.getEmail())
                .avatarUrl(e.getAvatarUrl())
                .showSpeedWidget(e.getShowSpeedWidget())
                .showFuelWidget(e.getShowFuelWidget())
                .showPerformanceWidget(e.getShowPerformanceWidget())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private NotificationSettingsDTO toNotificationDto(UserSettings e) {
        return NotificationSettingsDTO.builder()
                .enableNotifications(e.getEnableNotifications())
                .emailAlerts(e.getEmailAlerts())
                .smsAlerts(e.getSmsAlerts())
                .build();
    }

    private SecuritySettingsDTO toSecurityDto(UserSettings e) {
        return SecuritySettingsDTO.builder()
                .twoFactorEnabled(e.getTwoFactorEnabled())
                .phoneNumber(e.getPhoneNumber())
                .build();
    }
}
