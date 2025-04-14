package com.banenor.controller;

import com.banenor.dto.UserSettingsDTO;
import com.banenor.model.UserSettings;
import com.banenor.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/settings")
@Tag(name = "User Settings", description = "Endpoints for retrieving and updating user settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * GET endpoint to retrieve user settings.
     * (Note: In a production system, the user id should be extracted from the security context.)
     *
     * @return a Mono emitting ResponseEntity containing the user settings DTO.
     */
    @Operation(summary = "Get User Settings", description = "Retrieve the current user settings")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<UserSettingsDTO>> getUserSettings() {
        Long userId = 1L;  // For demonstration; extract from security context in production.
        log.info("GET /api/v1/users/settings called for userId: {}", userId);
        return settingsService.getUserSettings(userId)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .doOnError(ex -> log.error("Error fetching settings for userId {}: {}", userId, ex.getMessage()))
                .onErrorResume(ex -> Mono.just(ResponseEntity.status(500).build()));
    }

    /**
     * PUT endpoint to update user settings.
     *
     * @param settingsDTO the settings payload as DTO.
     * @return a Mono emitting ResponseEntity containing the updated settings DTO.
     */
    @Operation(summary = "Update User Settings", description = "Update the current user settings")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<UserSettingsDTO>> updateUserSettings(@RequestBody UserSettingsDTO settingsDTO) {
        Long userId = 1L;  // For demonstration; extract from security context in production.
        log.info("PUT /api/v1/users/settings called for userId: {} with payload: {}", userId, settingsDTO);
        // Convert incoming DTO to domain model for service processing.
        UserSettings settingsModel = convertToModel(settingsDTO);
        return settingsService.updateUserSettings(userId, settingsModel)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .doOnError(ex -> log.error("Error updating settings for userId {}: {}", userId, ex.getMessage()))
                .onErrorResume(ex -> Mono.just(ResponseEntity.status(500).build()));
    }

    // ----------------------------------------------------------------------------
    // Helper Methods for Converting Between Domain Model and DTO
    // ----------------------------------------------------------------------------

    /**
     * Converts a domain model UserSettings to a UserSettingsDTO.
     *
     * @param settings the domain model.
     * @return the UserSettingsDTO.
     */
    private UserSettingsDTO convertToDTO(UserSettings settings) {
        if (settings == null) {
            return null;
        }
        // Build General settings DTO
        UserSettingsDTO.GeneralSettingsDTO general = new UserSettingsDTO.GeneralSettingsDTO(
                settings.getUsername(),
                settings.getEmail(),
                settings.getAvatarUrl()
        );
        // For dashboard widget settings, use getters and default to false if null.
        UserSettingsDTO.DashboardSettingsDTO dashboard = new UserSettingsDTO.DashboardSettingsDTO(
                settings.getShowSpeedWidget() != null ? settings.getShowSpeedWidget() : false,
                settings.getShowFuelWidget() != null ? settings.getShowFuelWidget() : false,
                settings.getShowPerformanceWidget() != null ? settings.getShowPerformanceWidget() : false
        );
        // For notification settings, use getters and default to false if null.
        UserSettingsDTO.NotificationSettingsDTO notification = new UserSettingsDTO.NotificationSettingsDTO(
                settings.getEnableNotifications() != null ? settings.getEnableNotifications() : false,
                settings.getEmailAlerts() != null ? settings.getEmailAlerts() : false,
                settings.getSmsAlerts() != null ? settings.getSmsAlerts() : false
        );
        // For security settings, similarly ensure a default value.
        UserSettingsDTO.SecuritySettingsDTO security = new UserSettingsDTO.SecuritySettingsDTO(
                settings.getTwoFactorEnabled() != null ? settings.getTwoFactorEnabled() : false,
                settings.getPhoneNumber()
        );
        return new UserSettingsDTO(general, dashboard, notification, security);
    }

    /**
     * Converts a UserSettingsDTO to a domain model UserSettings.
     *
     * @param dto the UserSettingsDTO.
     * @return the domain model UserSettings.
     */
    private UserSettings convertToModel(UserSettingsDTO dto) {
        if (dto == null) {
            return null;
        }
        UserSettings settings = new UserSettings();
        settings.setUsername(dto.getGeneral().getUsername());
        settings.setEmail(dto.getGeneral().getEmail());
        settings.setAvatarUrl(dto.getGeneral().getAvatarUrl());
        settings.setShowSpeedWidget(dto.getDashboard().isShowSpeedWidget());
        settings.setShowFuelWidget(dto.getDashboard().isShowFuelWidget());
        settings.setShowPerformanceWidget(dto.getDashboard().isShowPerformanceWidget());
        settings.setEnableNotifications(dto.getNotification().isEnableNotifications());
        settings.setEmailAlerts(dto.getNotification().isEmailAlerts());
        settings.setSmsAlerts(dto.getNotification().isSmsAlerts());
        settings.setTwoFactorEnabled(dto.getSecurity().isTwoFactorEnabled());
        settings.setPhoneNumber(dto.getSecurity().getPhoneNumber());
        return settings;
    }
}
