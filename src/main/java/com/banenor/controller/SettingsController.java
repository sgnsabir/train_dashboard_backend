package com.banenor.controller;

import com.banenor.dto.*;
import com.banenor.service.SettingsService;
import com.banenor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/settings")
@Tag(name = "User Settings", description = "Endpoints for retrieving and updating the current user's settings")
@RequiredArgsConstructor
@Validated
public class SettingsController {

    private final SettingsService settingsService;
    private final UserService userService;

    @Operation(summary = "Get User Settings", description = "Retrieve the current user’s aggregated settings")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<UserSettingsDTO>> getUserSettings(
            @AuthenticationPrincipal UserDetails principal) {

        return userService.getUserProfile(principal.getUsername())
                .flatMap(profile ->
                        settingsService.getUserSettings(profile.getId())
                                .map(dashboard -> mapToUserSettingsDTO(profile, dashboard))
                )
                .map(settings -> ResponseEntity.ok(settings))
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(ex -> {
                    log.error("Error fetching settings for user {}: {}", principal.getUsername(), ex.getMessage(), ex);
                })
                .onErrorResume(ex -> Mono.just(ResponseEntity.status(500).build()));
    }

    @Operation(summary = "Update User Settings",
            description = "Update the current user’s general, dashboard, notification, and security settings")
    @PreAuthorize("isAuthenticated()")
    @PutMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<UserSettingsDTO>> updateUserSettings(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UserSettingsDTO incoming) {

        return userService.getUserProfile(principal.getUsername())
                .flatMap(profile -> {
                    Long userId = profile.getId();

                    Mono<DashboardSettingsDTO> updatedDashboard =
                            settingsService.updateUserSettings(userId, incoming.getDashboard());

                    Mono<UserDTO> updatedProfile =
                            userService.updateOwnProfile(
                                    principal.getUsername(),
                                    incoming.getGeneral().getEmail(),
                                    incoming.getGeneral().getAvatarUrl(),
                                    incoming.getSecurity().getPhoneNumber()
                            );

                    return Mono.zip(updatedProfile, updatedDashboard);
                })
                .map(tuple -> mapToUserSettingsDTO(tuple.getT1(), tuple.getT2()))
                .map(settings -> ResponseEntity.ok(settings))
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(ex -> {
                    log.error("Error updating settings for user {}: {}", principal.getUsername(), ex.getMessage(), ex);
                })
                .onErrorResume(ex -> Mono.just(ResponseEntity.status(500).build()));
    }

    //───────────────────────────────────────────────────────────────────────────────────
    // Private helper to assemble the wrapper DTO from profile + dashboard pieces
    //───────────────────────────────────────────────────────────────────────────────────

    private UserSettingsDTO mapToUserSettingsDTO(UserDTO profile,
                                                 DashboardSettingsDTO dashboard) {
        return UserSettingsDTO.builder()
                .general(
                        GeneralSettingsDTO.builder()
                                .username(profile.getUsername())
                                .email(profile.getEmail())
                                .avatarUrl(profile.getAvatarUrl())
                                .build()
                )
                .dashboard(dashboard)
                .notification(
                        NotificationSettingsDTO.builder()
                                .enableNotifications(dashboard.getEnableNotifications())
                                .emailAlerts(dashboard.getEmailAlerts())
                                .smsAlerts(dashboard.getSmsAlerts())
                                .build()
                )
                .security(
                        SecuritySettingsDTO.builder()
                                .twoFactorEnabled(dashboard.getTwoFactorEnabled())
                                .phoneNumber(dashboard.getPhoneNumber())
                                .build()
                )
                .build();
    }
}
