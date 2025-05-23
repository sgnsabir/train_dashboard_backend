package com.banenor.service;

import com.banenor.dto.*;
import com.banenor.model.User;
import reactor.core.publisher.Mono;

public interface AuthService {
    Mono<User> register(RegistrationRequest registrationRequest);

    Mono<Void> verifyToken(String token);

    Mono<AuthResponse> login(LoginRequest loginRequest);

    Mono<Void> logout(String token);

    Mono<Void> resetPassword(PasswordResetRequest passwordResetRequest);

    Mono<Void> changePassword(PasswordChangeRequest changePasswordRequest);
}
