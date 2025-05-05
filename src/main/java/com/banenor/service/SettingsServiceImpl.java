package com.banenor.service;

import com.banenor.dto.DashboardSettingsDTO;
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
    public Mono<DashboardSettingsDTO> getUserSettings(Long userId) {
        return repo.findById(userId)
                .defaultIfEmpty(createDefault(userId))
                .map(this::toDto)
                .doOnSuccess(dto -> log.info("Fetched settings for userId={}", userId));
    }

    @Override
    public Mono<DashboardSettingsDTO> updateUserSettings(Long userId, DashboardSettingsDTO dto) {
        if (!userId.equals(dto.getUserId())) {
            return Mono.error(new ValidationException("Path userId and body.userId must match"));
        }

        return repo.findById(userId)
                .defaultIfEmpty(createDefault(userId))
                .flatMap(entity -> {
                    // overlay allowed fields
                    entity.setShowSpeedWidget(dto.getShowSpeedWidget());
                    entity.setShowFuelWidget(dto.getShowFuelWidget());
                    entity.setShowPerformanceWidget(dto.getShowPerformanceWidget());
                    entity.setEnableNotifications(dto.getEnableNotifications());
                    entity.setEmailAlerts(dto.getEmailAlerts());
                    entity.setSmsAlerts(dto.getSmsAlerts());
                    entity.setTwoFactorEnabled(dto.getTwoFactorEnabled());
                    entity.setPhoneNumber(dto.getPhoneNumber());
                    entity.setUpdatedAt(LocalDateTime.now());
                    return repo.save(entity);
                })
                .map(this::toDto)
                .doOnSuccess(updated -> log.info("Updated settings for userId={}", userId));
    }

    private UserSettings createDefault(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return UserSettings.builder()
                .userId(userId)
                .showSpeedWidget(true)
                .showFuelWidget(true)
                .showPerformanceWidget(true)
                .enableNotifications(true)
                .emailAlerts(true)
                .smsAlerts(false)
                .twoFactorEnabled(false)
                .phoneNumber("")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private DashboardSettingsDTO toDto(UserSettings e) {
        return DashboardSettingsDTO.builder()
                .userId(e.getUserId())
                .username(e.getUsername())
                .email(e.getEmail())
                .avatarUrl(e.getAvatarUrl())
                .showSpeedWidget(e.getShowSpeedWidget())
                .showFuelWidget(e.getShowFuelWidget())
                .showPerformanceWidget(e.getShowPerformanceWidget())
                .enableNotifications(e.getEnableNotifications())
                .emailAlerts(e.getEmailAlerts())
                .smsAlerts(e.getSmsAlerts())
                .twoFactorEnabled(e.getTwoFactorEnabled())
                .phoneNumber(e.getPhoneNumber())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
