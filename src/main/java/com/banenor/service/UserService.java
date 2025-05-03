package com.banenor.service;

import com.banenor.controller.ProfileController;
import com.banenor.dto.UserResponse;
import com.banenor.dto.UserUpdateRequest;
import com.banenor.model.User;
import com.banenor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Mono<UserResponse> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::mapToUserResponse);
    }

    public Mono<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::mapToUserResponse);
    }

    public Flux<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .map(this::mapToUserResponse);
    }

    public Mono<UserResponse> updateUser(Long id, UserUpdateRequest request) {
        return userRepository.findById(id)
                .flatMap(existingUser -> {
                    if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
                        existingUser.setUsername(request.getUsername().trim());
                    }
                    if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                        existingUser.setEmail(request.getEmail().trim());
                    }
                    return userRepository.save(existingUser);
                })
                .map(this::mapToUserResponse);
    }

    public Mono<Void> deleteUser(Long id) {
        return userRepository.deleteById(id);
    }

    public Mono<UserResponse> updateProfile(String username, ProfileController.ProfileUpdateRequest request) {
        return userRepository.findByUsername(username)
                .flatMap(existingUser -> {
                    if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                        existingUser.setEmail(request.getEmail().trim());
                    }
                    if (request.getAvatar() != null && !request.getAvatar().trim().isEmpty()) {
                        existingUser.setAvatar(request.getAvatar().trim());
                    }
                    if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
                        existingUser.setPhone(request.getPhone().trim());
                    }
                    return userRepository.save(existingUser);
                })
                .map(this::mapToUserResponse);
    }

    public Mono<UserResponse> updateAvatar(Long userId, String avatarUrl) {
        return userRepository.findById(userId)
                .flatMap(existingUser -> {
                    existingUser.setAvatar(avatarUrl);
                    return userRepository.save(existingUser);
                })
                .map(this::mapToUserResponse);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAvatar(user.getAvatar());
        response.setPhone(user.getPhone());
        response.setTwoFactorEnabled(user.getTwoFactorEnabled());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        List<String> roles = user.getRole() != null
                ? Collections.singletonList(user.getRole())
                : Collections.emptyList();
        response.setRoles(roles);

        return response;
    }
}
