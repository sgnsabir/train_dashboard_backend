package com.banenor.service;

import com.banenor.controller.ProfileController;
import com.banenor.dto.UserResponse;
import com.banenor.dto.UserUpdateRequest;
import com.banenor.model.User;
import com.banenor.repository.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
                .flatMap(user -> {
                    user.setUsername(request.getUsername());
                    user.setEmail(request.getEmail());
                    return userRepository.save(user);
                })
                .map(this::mapToUserResponse);
    }

    public Mono<Void> deleteUser(Long id) {
        return userRepository.deleteById(id);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAvatar(user.getAvatar());
        response.setPhone(user.getPhone());
        // Convert the single role string into a singleton list
        response.setRoles(Collections.singletonList(user.getRole()));
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    /**
     * Updates the profile of the currently authenticated user.
     * Only allowed fields (email, avatar, and phone) are updated.
     *
     * @param username the username of the current user
     * @param request  the profile update request containing new email, avatar, and phone
     * @return a Mono emitting the updated UserResponse
     */
    public Mono<UserResponse> updateProfile(String username, ProfileController.ProfileUpdateRequest request) {
        return userRepository.findByUsername(username)
                .flatMap(user -> {
                    if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                        user.setEmail(request.getEmail());
                    }
                    if (request.getAvatar() != null && !request.getAvatar().trim().isEmpty()) {
                        user.setAvatar(request.getAvatar());
                    }
                    if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
                        user.setPhone(request.getPhone());
                    }
                    return userRepository.save(user);
                })
                .map(this::mapToUserResponse);
    }

    /**
     * Updates the avatar URL of the user with the given userId.
     *
     * @param userId    the user's ID.
     * @param avatarUrl the new avatar URL.
     * @return a Mono emitting the updated UserResponse.
     */
    public Mono<UserResponse> updateAvatar(Long userId, String avatarUrl) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setAvatar(avatarUrl);
                    return userRepository.save(user);
                })
                .map(this::mapToUserResponse);
    }
}
