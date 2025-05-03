package com.banenor.service;

import com.banenor.dto.LoginRequest;
import com.banenor.dto.LoginResponse;
import com.banenor.dto.PasswordResetRequest;
import com.banenor.dto.RegistrationRequest;
import com.banenor.model.User;
import reactor.core.publisher.Mono;

/**
 * Service interface for handling user authentication and account management.
 */
public interface AuthService {

    Mono<User> register(RegistrationRequest registrationRequest);

    Mono<LoginResponse> login(LoginRequest loginRequest);

    Mono<Void> logout(String token);

    Mono<Void> resetPassword(PasswordResetRequest passwordResetRequest);

    Mono<Void> changePassword(String username, String oldPassword, String newPassword);
}
