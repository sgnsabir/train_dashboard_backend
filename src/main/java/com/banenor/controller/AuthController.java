package com.banenor.controller;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banenor.dto.ChangePasswordRequest;
import com.banenor.dto.LoginRequest;
import com.banenor.dto.LoginResponse;
import com.banenor.dto.PasswordResetRequest;
import com.banenor.dto.RegistrationRequest;
import com.banenor.dto.UserResponse;
import com.banenor.security.JwtUtil;
import com.banenor.service.AuthService;
import com.banenor.service.UserService;

import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "Authentication", description = "Endpoints for user authentication and account management")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, UserService userService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    @Operation(
            summary = "User Registration (Admin Only)",
            description = "Register a new user account. Only authenticated admin users are allowed to create new users."
    )
    @ApiResponse(responseCode = "201", description = "User registered and authenticated successfully")
    @ApiResponse(responseCode = "403", description = "Access forbidden: Only admin users may register new accounts")
    @ApiResponse(responseCode = "400", description = "Validation error or user already exists")
    public Mono<ResponseEntity<LoginResponse>> register(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            @Valid @RequestBody RegistrationRequest registrationRequest) {

        // Ensure that the caller is authenticated and has the "ROLE_ADMIN" authority.
        if (userDetails == null || userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .noneMatch(authority -> authority.equals("ROLE_ADMIN"))) {
            log.warn("Unauthorized registration attempt by user: {}",
                    userDetails != null ? userDetails.getUsername() : "anonymous");
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }

        log.debug("Registration request received for username: {} by admin: {}",
                registrationRequest.getUsername(), userDetails.getUsername());

        return authService.register(registrationRequest)
                .flatMap(user -> {
                    // After registration, automatically log in the new user.
                    LoginRequest loginRequest = new LoginRequest();
                    loginRequest.setUsername(registrationRequest.getUsername());
                    loginRequest.setPassword(registrationRequest.getPassword());
                    return authService.login(loginRequest);
                })
                .map(loginResponse -> {
                    log.debug("Registration and auto-login successful for username: {}", registrationRequest.getUsername());
                    return ResponseEntity.status(HttpStatus.CREATED).body(loginResponse);
                })
                .doOnError(ex -> log.error("Registration/auto-login failed for user '{}': {}",
                        registrationRequest.getUsername(), ex.getMessage(), ex));
    }

    @PostMapping("/login")
    @Operation(
            summary = "User Login",
            description = "Authenticate a user and return a JWT token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public Mono<ResponseEntity<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.debug("Login request received for username: {}", loginRequest.getUsername());
        return authService.login(loginRequest)
                .map(jwtResponse -> {
                    log.debug("Login successful for username: {}", loginRequest.getUsername());
                    return ResponseEntity.ok(jwtResponse);
                })
                .doOnError(ex -> log.error("Login failed for username '{}': {}",
                        loginRequest.getUsername(), ex.getMessage(), ex));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "User Logout",
            description = "Invalidate a user's JWT token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Mono<ResponseEntity<Void>> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return authService.logout(token)
                .then(Mono.<ResponseEntity<Void>>fromSupplier(() -> {
                    log.debug("Logout successful");
                    return ResponseEntity.ok().build();
                }))
                .doOnError(ex -> log.error("Logout failed: {}", ex.getLocalizedMessage()));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset Password",
            description = "Reset the password for a user account"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Validation error or user not found")
    })
    public Mono<ResponseEntity<String>> resetPassword(@Valid @RequestBody PasswordResetRequest passwordResetRequest) {
        return authService.resetPassword(passwordResetRequest)
                .thenReturn(ResponseEntity.ok("Password reset successful"))
                .doOnError(ex -> log.error("Password reset failed for email '{}': {}",
                        passwordResetRequest.getEmail(), ex.getLocalizedMessage()));
    }

    // --- Modified getCurrentUser endpoint ---
    @GetMapping("/me")
    @Operation(
            summary = "Get Current User Profile",
            description = "Retrieve the profile details of the currently authenticated user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Mono<ResponseEntity<UserResponse>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        // If the security context has a user, use it
        if (userDetails != null) {
            String principal = userDetails.getUsername();
            try {
                return userService.getUserById(Long.valueOf(principal))
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build())
                        .doOnError(ex -> log.error("Failed to retrieve user by ID '{}': {}", principal, ex.getLocalizedMessage()));
            } catch (NumberFormatException e) {
                return userService.getUserByUsername(principal)
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build())
                        .doOnError(ex -> log.error("Failed to retrieve user by username '{}': {}", principal, ex.getLocalizedMessage()));
            }
        }
        // If not, try to extract the token from the Authorization header
        else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = null;
            try {
                username = jwtUtil.getUsernameFromToken(token);
            } catch (ExpiredJwtException ex) {
                username = ex.getClaims().getSubject();
                log.info("Token expired, extracting username from expired token: {}", username);
            } catch (Exception ex) {
                log.error("Failed to parse token during /me: {}", ex.getMessage(), ex);
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }
            if (username == null || username.trim().isEmpty()) {
                log.warn("Username not found in token during /me");
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }
            try {
                return userService.getUserById(Long.valueOf(username))
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build());
            } catch (NumberFormatException e) {
                return userService.getUserByUsername(username)
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build());
            }
        } else {
            log.warn("No user is authenticated; returning 401");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
    }
    // --- End of modified getCurrentUser endpoint ---

    @PostMapping("/change-password")
    @Operation(
            summary = "Change Password",
            description = "Change the password for the currently authenticated user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or incorrect current password"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Mono<ResponseEntity<String>> changePassword(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        if (userDetails == null) {
            log.warn("Attempt to change password with no authenticated user; returning 401");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));
        }
        return authService.changePassword(userDetails.getUsername(), request.getOldPassword(), request.getNewPassword())
                .thenReturn(ResponseEntity.ok("Password changed successfully"))
                .doOnError(ex -> log.error("Password change failed for user '{}': {}",
                        userDetails.getUsername(), ex.getLocalizedMessage()));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh Token",
            description = "Generate a new JWT token based on an expired or about-to-expire token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized or token refresh failed")
    })
    public Mono<ResponseEntity<LoginResponse>> refreshToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Authorization header missing or invalid for token refresh");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        String oldToken = authHeader.substring(7);
        String username;
        try {
            username = jwtUtil.getUsernameFromToken(oldToken);
        } catch (ExpiredJwtException ex) {
            username = ex.getClaims().getSubject();
            log.info("Token expired, extracting username from expired token: {}", username);
        } catch (Exception ex) {
            log.error("Failed to parse token during refresh: {}", ex.getMessage(), ex);
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        if (username == null || username.trim().isEmpty()) {
            log.warn("Username not found in token during refresh");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        final String finalUsername = username;
        return userService.getUserByUsername(finalUsername)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found with username: " + finalUsername)))
                .flatMap(userResponse -> {
                    String newToken = jwtUtil.generateToken(finalUsername, userResponse.getRoles().toString());
                    Date expirationDate = jwtUtil.getExpirationDateFromToken(newToken);
                    long expiresIn = (expirationDate.getTime() - System.currentTimeMillis()) / 1000;
                    LoginResponse loginResponse = new LoginResponse();
                    loginResponse.setToken(newToken);
                    loginResponse.setUsername(finalUsername);
                    loginResponse.setExpiresIn(expiresIn);
                    log.info("Token refreshed successfully for user: {}", finalUsername);
                    return Mono.just(ResponseEntity.ok(loginResponse));
                })
                .doOnError(ex -> log.error("Error refreshing token for user {}: {}", finalUsername, ex.getMessage(), ex));
    }
}
