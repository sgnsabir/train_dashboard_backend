// src/main/java/com/banenor/service/UserService.java
package com.banenor.service;

import com.banenor.controller.ProfileController.ProfileUpdateRequest;
import com.banenor.dto.UserProfileDTO;
import com.banenor.dto.UserResponse;
import com.banenor.dto.UserUpdateRequest;
import com.banenor.model.User;
import com.banenor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Mono<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::mapToUserResponse)
                .doOnError(e -> log.error("Error fetching user by username {}: {}", username, e.getMessage(), e));
    }

    public Mono<UserResponse> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::mapToUserResponse)
                .doOnError(e -> log.error("Error fetching user by id {}: {}", id, e.getMessage(), e));
    }

    public Mono<User> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + username)))
                .doOnError(e -> log.error("Error fetching user entity by username {}: {}", username, e.getMessage(), e));
    }

    public Mono<User> findEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(
                        new UsernameNotFoundException("User not found: " + username)))
                .doOnError(e -> log.error(
                        "Error fetching raw user entity for {}: {}", username, e.getMessage(), e));
    }


    public Flux<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .map(this::mapToUserResponse)
                .doOnComplete(() -> log.info("Fetched all users"))
                .doOnError(e -> log.error("Error fetching all users: {}", e.getMessage(), e));
    }

    public Mono<UserResponse> updateUser(Long id, UserUpdateRequest request) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found with ID: " + id)))
                .flatMap(user -> {
                    if (request.getUsername() != null && !request.getUsername().isBlank()) {
                        user.setUsername(request.getUsername().trim());
                    }
                    if (request.getEmail() != null && !request.getEmail().isBlank()) {
                        user.setEmail(request.getEmail().trim());
                    }
                    if (request.getPhone() != null) {
                        user.setPhone(request.getPhone().trim());
                    }
                    if (request.getAvatar() != null) {
                        user.setAvatar(request.getAvatar().trim());
                    }
                    return userRepository.save(user);
                })
                .map(this::mapToUserResponse)
                .doOnSuccess(u -> log.info("Updated user with ID {}", id))
                .doOnError(ex -> log.error("Error updating user with ID {}: {}", id, ex.getMessage(), ex));
    }


    public Mono<Void> deleteUser(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found with ID: " + id)))
                .flatMap(user -> userRepository.delete(user))
                .doOnSuccess(v -> log.info("Deleted user with ID {}", id))
                .doOnError(ex -> log.error("Error deleting user with ID {}: {}", id, ex.getMessage(), ex));
    }


    public Mono<UserProfileDTO> getProfileById(Long id) {
        return userRepository.findById(id)
                .map(this::toDto)
                .doOnError(ex -> log.error("Error fetching user by id {}: {}", id, ex.getMessage(), ex));
    }


    public Mono<UserProfileDTO> getProfileByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toDto)
                .doOnError(ex -> log.error("Error fetching user by username {}: {}", username, ex.getMessage(), ex));
    }


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


    public Mono<UserProfileDTO> getUserProfile(String username) {
        return userRepository.findByUsername(username)
                .map(this::mapToUserProfileDTO)
                .doOnError(e -> log.error("Error fetching profile for {}: {}", username, e.getMessage(), e));
    }


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


    public Mono<UserProfileDTO> updateAvatar(Long userId, String avatarUrl) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found: " + userId)))
                .flatMap(user -> {
                    user.setAvatar(avatarUrl.trim());
                    return userRepository.save(user);
                })
                .map(this::toDto)
                .doOnSuccess(dto -> log.info("Avatar updated for user {}", userId))
                .doOnError(ex -> log.error("Error updating avatar for user {}: {}", userId, ex.getMessage(), ex));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private mapping helpers
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
