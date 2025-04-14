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

    /**
     * Registers a new user.
     *
     * @param registrationRequest the registration request containing username, email, and password.
     * @return a Mono emitting the newly created User.
     */
    Mono<User> register(RegistrationRequest registrationRequest);

    /**
     * Authenticates a user and returns a JWT token wrapped in a LoginResponse.
     *
     * @param loginRequest the login request containing username and password.
     * @return a Mono emitting the LoginResponse that includes the token, username, and expiration details.
     */
    Mono<LoginResponse> login(LoginRequest loginRequest);

    /**
     * Logs out a user by invalidating the provided JWT token.
     *
     * @param token the JWT token to be invalidated.
     * @return a Mono that completes when logout is done.
     */
    Mono<Void> logout(String token);

    /**
     * Resets the password for a user.
     *
     * @param passwordResetRequest the request containing the user's email and new password.
     * @return a Mono that completes when the password is reset.
     */
    Mono<Void> resetPassword(PasswordResetRequest passwordResetRequest);

    /**
     * Changes the password for the currently authenticated user.
     *
     * @param username    the username of the user.
     * @param oldPassword the current password.
     * @param newPassword the new password to be set.
     * @return a Mono that completes when the password is successfully changed.
     */
    Mono<Void> changePassword(String username, String oldPassword, String newPassword);
}
