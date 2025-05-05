package com.banenor.service;

import com.banenor.controller.ProfileController.ProfileUpdateRequest;
import com.banenor.dto.UserProfileDTO;
import com.banenor.dto.UserResponse;
import com.banenor.model.User;
import com.banenor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    /**
     * For AuthController: load UserResponse by username.
     */
    public Mono<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::mapToUserResponse)
                .doOnError(e -> log.error("Error fetching user by username {}: {}", username, e.getMessage(), e));
    }

    /**
     * For AuthController: load UserResponse by user ID.
     */
    public Mono<UserResponse> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::mapToUserResponse)
                .doOnError(e -> log.error("Error fetching user by id {}: {}", id, e.getMessage(), e));
    }

    /**
     * If you need the raw User entity (e.g. to pull roles or other fields).
     */
    public Mono<User> findEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .doOnError(e -> log.error("Error fetching user entity {}: {}", username, e.getMessage(), e));
    }

    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Fetch a UserProfileDTO by user ID.
     */
    public Mono<UserProfileDTO> getProfileById(Long id) {
        return userRepository.findById(id)
                .map(this::toDto)
                .doOnError(ex -> log.error("Error fetching user by id {}: {}", id, ex.getMessage(), ex));
    }

    /**
     * Fetch a UserProfileDTO by username.
     */
    public Mono<UserProfileDTO> getProfileByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toDto)
                .doOnError(ex -> log.error("Error fetching user by username {}: {}", username, ex.getMessage(), ex));
    }

    /**
     * Update only email/avatar/phone on the user’s own profile.
     */
    public Mono<UserProfileDTO> updateOwnProfile(String username,
                                                 String email,
                                                 String avatar,
                                                 String phone) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found: " + username)))
                .flatMap(user -> {
                    if (email != null && !email.isBlank()) {
                        user.setEmail(email.trim());
                    }
                    if (avatar != null && !avatar.isBlank()) {
                        user.setAvatar(avatar.trim());
                    }
                    if (phone != null && !phone.isBlank()) {
                        user.setPhone(phone.trim());
                    }
                    return userRepository.save(user);
                })
                .map(this::toDto)
                .doOnSuccess(u -> log.info("User {} updated their profile", username))
                .doOnError(ex -> log.error("Error updating profile for {}: {}", username, ex.getMessage(), ex));
    }

    /**
     * Public profile lookup used by ProfileController.
     */
    public Mono<UserProfileDTO> getUserProfile(String username) {
        return userRepository.findByUsername(username)
                .map(this::mapToUserProfileDTO)
                .doOnError(e -> log.error("Error fetching profile for {}: {}", username, e.getMessage(), e));
    }

    /**
     * Public profile update used by ProfileController.
     */
    public Mono<UserProfileDTO> updateUserProfile(String username, ProfileUpdateRequest req) {
        return userRepository.findByUsername(username)
                .flatMap(user -> {
                    if (req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
                        user.setEmail(req.getEmail().trim());
                    }
                    if (req.getAvatar() != null && !req.getAvatar().trim().isEmpty()) {
                        user.setAvatar(req.getAvatar().trim());
                    }
                    if (req.getPhone() != null && !req.getPhone().trim().isEmpty()) {
                        user.setPhone(req.getPhone().trim());
                    }
                    return userRepository.save(user);
                })
                .map(this::mapToUserProfileDTO)
                .doOnError(e -> log.error("Error updating profile for {}: {}", username, e.getMessage(), e));
    }

    // ──────────────────────────────────────────────────────────────────────────

    private UserProfileDTO toDto(User user) {
        return UserProfileDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatar())
                .phoneNumber(user.getPhone())
                .roles(user.getRole() != null
                        ? Collections.singletonList(user.getRole())
                        : Collections.emptyList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private UserProfileDTO mapToUserProfileDTO(User user) {
        List<String> roles = user.getRole() != null
                ? Collections.singletonList(user.getRole())
                : Collections.emptyList();

        return UserProfileDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatar())
                .phoneNumber(user.getPhone())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse resp = new UserResponse();
        resp.setId(user.getUserId());
        resp.setUsername(user.getUsername());
        resp.setEmail(user.getEmail());
        resp.setAvatar(user.getAvatar());
        resp.setPhone(user.getPhone());
        resp.setTwoFactorEnabled(user.getTwoFactorEnabled());
        resp.setCreatedAt(user.getCreatedAt());
        resp.setUpdatedAt(user.getUpdatedAt());

        List<String> roles = user.getRole() != null
                ? Collections.singletonList(user.getRole())
                : Collections.emptyList();
        resp.setRoles(roles);

        return resp;
    }
}
