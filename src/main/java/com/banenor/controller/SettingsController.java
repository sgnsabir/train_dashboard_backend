package com.banenor.controller;

import com.banenor.dto.*;
import com.banenor.service.SettingsService;
import com.banenor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/settings")
@Tag(name = "User Settings", description = "Endpoints for retrieving and updating the current user's settings")
@RequiredArgsConstructor
@Validated
public class SettingsController {

    private final SettingsService settingsService;
    private final UserService userService;

    @Operation(summary = "Get User Settings",
            description = "Retrieve the current user's general, dashboard, notification, and security settings")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<UserSettingsDTO>> getUserSettings(
            @AuthenticationPrincipal UserDetails principal) {

        String username = principal.getUsername();
        log.debug("GET /users/settings for '{}'", username);

        return userService.getUserProfile(username)
                .switchIfEmpty(Mono.error(new IllegalStateException("User not found: " + username)))
                .flatMap(profile -> {
                    Long userId = profile.getId();

                    // Fetch all four setting groups in parallel
                    Mono<GeneralSettingsDTO> generalM =
                            settingsService.getGeneralSettings(userId)
                                    .doOnError(e -> log.error("Error loading general settings for userId={}", userId, e));

                    Mono<DashboardSettingsDTO> dashboardM =
                            settingsService.getDashboardSettings(userId)
                                    .doOnError(e -> log.error("Error loading dashboard settings for userId={}", userId, e));

                    Mono<NotificationSettingsDTO> notificationM =
                            settingsService.getNotificationSettings(userId)
                                    .doOnError(e -> log.error("Error loading notification settings for userId={}", userId, e));

                    Mono<SecuritySettingsDTO> securityM =
                            settingsService.getSecuritySettings(userId)
                                    .doOnError(e -> log.error("Error loading security settings for userId={}", userId, e));

                    return Mono.zip(generalM, dashboardM, notificationM, securityM)
                            .map(tuple -> UserSettingsDTO.builder()
                                    .general(tuple.getT1())
                                    .dashboard(tuple.getT2())
                                    .notification(tuple.getT3())
                                    .security(tuple.getT4())
                                    .build()
                            );
                })
                .map(ResponseEntity::ok)
                .doOnError(ex -> log.error("Failed to retrieve settings for '{}'", username, ex))
                .onErrorResume(ex -> {
                    // Return 404 for missing user, 500 otherwise
                    if (ex instanceof IllegalStateException) {
                        return Mono.just(ResponseEntity.notFound().build());
                    }
                    return Mono.just(ResponseEntity.status(500).build());
                });
    }

    @Operation(summary = "Update User Settings",
            description = "Update the current user's general, dashboard, notification, and security settings")
    @PreAuthorize("isAuthenticated()")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<UserSettingsDTO>> updateUserSettings(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UserSettingsDTO incoming) {

        String username = principal.getUsername();
        log.debug("PUT /users/settings for '{}': {}", username, incoming);

        return userService.getUserProfile(username)
                .switchIfEmpty(Mono.error(new IllegalStateException("User not found: " + username)))
                .flatMap(profile -> {
                    Long userId = profile.getId();

                    // Update all four setting groups in parallel
                    Mono<GeneralSettingsDTO> updatedGeneralM =
                            settingsService.updateGeneralSettings(userId, incoming.getGeneral())
                                    .doOnSuccess(g -> log.info("Updated general settings for userId={} → {}", userId, g))
                                    .doOnError(e -> log.error("Error updating general settings for userId={}", userId, e));

                    Mono<DashboardSettingsDTO> updatedDashboardM =
                            settingsService.updateDashboardSettings(userId, incoming.getDashboard())
                                    .doOnSuccess(d -> log.info("Updated dashboard settings for userId={} → {}", userId, d))
                                    .doOnError(e -> log.error("Error updating dashboard settings for userId={}", userId, e));

                    Mono<NotificationSettingsDTO> updatedNotificationM =
                            settingsService.updateNotificationSettings(userId, incoming.getNotification())
                                    .doOnSuccess(n -> log.info("Updated notification settings for userId={} → {}", userId, n))
                                    .doOnError(e -> log.error("Error updating notification settings for userId={}", userId, e));

                    Mono<SecuritySettingsDTO> updatedSecurityM =
                            settingsService.updateSecuritySettings(userId, incoming.getSecurity())
                                    .doOnSuccess(s -> log.info("Updated security settings for userId={} → {}", userId, s))
                                    .doOnError(e -> log.error("Error updating security settings for userId={}", userId, e));

                    return Mono.zip(updatedGeneralM, updatedDashboardM, updatedNotificationM, updatedSecurityM)
                            .map(tuple -> UserSettingsDTO.builder()
                                    .general(tuple.getT1())
                                    .dashboard(tuple.getT2())
                                    .notification(tuple.getT3())
                                    .security(tuple.getT4())
                                    .build()
                            );
                })
                .map(ResponseEntity::ok)
                .doOnError(ex -> log.error("Failed to update settings for '{}'", username, ex))
                .onErrorResume(ex -> {
                    if (ex instanceof IllegalStateException) {
                        return Mono.just(ResponseEntity.notFound().build());
                    }
                    return Mono.just(ResponseEntity.status(500).build());
                });
    }
}
