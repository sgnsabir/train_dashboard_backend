// src/main/java/com/banenor/controller/ProfileController.java
package com.banenor.controller;

import com.banenor.dto.UserDTO;
import com.banenor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    public Mono<ResponseEntity<UserDTO>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            log.warn("No authenticated user; returning 401");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return userService.getUserProfile(userDetails.getUsername())
                .map(dto -> ResponseEntity.ok(dto))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Update Profile",
            description = "Update profile details such as avatar, email, and phone for the currently authenticated user"
    )
    @PutMapping
    public Mono<ResponseEntity<UserDTO>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequest request) {

        if (userDetails == null) {
            log.warn("No authenticated user; cannot update profile");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return userService.updateUserProfile(userDetails.getUsername(), request)
                .map(dto -> ResponseEntity.ok(dto))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileUpdateRequest {
        @Email(message = "Invalid email format")
        private String email;

        /** URL for the user's avatar image. */
        private String avatar;

        /** Basic phone‐number validation */
        @Pattern(regexp = "^\\+?[0-9\\- ]{7,15}$", message = "Invalid phone number format")
        private String phone;
    }
}
