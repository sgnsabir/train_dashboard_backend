package com.banenor.controller;

import com.banenor.dto.UserResponse;
import com.banenor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "Profile", description = "Endpoints for managing the current user's profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Get Current User Profile",
            description = "Retrieve the profile details of the currently authenticated user"
    )
    @GetMapping
    public Mono<ResponseEntity<UserResponse>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("No authenticated user found; returning 401");
            return Mono.just(ResponseEntity.status(401).build());
        }
        return userService.getUserByUsername(userDetails.getUsername())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(ex -> log.error("Error retrieving profile for {}: {}", userDetails.getUsername(), ex.getMessage(), ex));
    }

    @Operation(
            summary = "Update Profile",
            description = "Update profile details such as avatar, email, and phone for the currently authenticated user"
    )
    @PutMapping
    public Mono<ResponseEntity<UserResponse>> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                                            @Valid @RequestBody ProfileUpdateRequest request) {
        if (userDetails == null) {
            log.warn("No authenticated user found; cannot update profile");
            return Mono.just(ResponseEntity.status(401).build());
        }
        String username = userDetails.getUsername();
        return userService.updateProfile(username, request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(ex -> log.error("Error updating profile for {}: {}", username, ex.getMessage(), ex));
    }

    @Data
    public static class ProfileUpdateRequest {
        @Email(message = "Invalid email format")
        private String email;

        // URL for the user's avatar image.
        // (Consider adding additional URL validation if required.)
        private String avatar;

        // Phone number – basic regex to validate common formats.
        @Pattern(regexp = "^\\+?[0-9\\- ]{7,15}$", message = "Invalid phone number format")
        private String phone;
    }
}
