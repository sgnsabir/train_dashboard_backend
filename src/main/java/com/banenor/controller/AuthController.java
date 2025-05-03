package com.banenor.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.banenor.dto.*;
import com.banenor.security.JwtTokenUtil;
import com.banenor.service.AuthService;
import com.banenor.service.UserService;

import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and account management")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/register")
    public Mono<ResponseEntity<LoginResponse>> register(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            @Valid @RequestBody RegistrationRequest registrationRequest) {

        if (userDetails == null || userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .noneMatch(auth -> auth.equals("ROLE_ADMIN"))) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }

        return authService.register(registrationRequest)
                .flatMap(user -> {
                    LoginRequest loginRequest = new LoginRequest();
                    loginRequest.setUsername(user.getUsername());
                    loginRequest.setPassword(registrationRequest.getPassword());
                    return authService.login(loginRequest);
                })
                .map(token -> ResponseEntity.status(HttpStatus.CREATED).body(token));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final String token = authHeader.substring(7);
            return authService.logout(token)
                    .thenReturn(ResponseEntity.ok().build());
        }
        return Mono.just(ResponseEntity.ok().build());
    }

    @PostMapping("/reset-password")
    public Mono<ResponseEntity<String>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        return authService.resetPassword(request)
                .thenReturn(ResponseEntity.ok("Password reset successful"));
    }

    @PostMapping("/change-password")
    public Mono<ResponseEntity<String>> changePassword(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        if (userDetails == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));
        }

        return authService.changePassword(userDetails.getUsername(), request.getOldPassword(), request.getNewPassword())
                .thenReturn(ResponseEntity.ok("Password changed successfully"));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<LoginResponse>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        final String token = authHeader.substring(7);
        String extractedUsername;

        try {
            extractedUsername = jwtTokenUtil.getUsernameFromToken(token);
        } catch (ExpiredJwtException e) {
            extractedUsername = e.getClaims().getSubject();
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        if (extractedUsername == null || extractedUsername.trim().isEmpty()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        final String username = extractedUsername;

        return userService.getUserByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + username)))
                .flatMap(user -> {
                    List<String> roles = user.getRoles() != null ? user.getRoles() : Collections.emptyList();
                    String newToken = jwtTokenUtil.generateToken(username, roles);
                    long expiresIn = (jwtTokenUtil.getExpirationDateFromToken(newToken).getTime() - System.currentTimeMillis()) / 1000;

                    LoginResponse response = new LoginResponse();
                    response.setToken(newToken);
                    response.setUsername(username);
                    response.setExpiresIn(expiresIn);

                    return Mono.just(ResponseEntity.ok(response));
                });
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserResponse>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        if (userDetails != null) {
            final String principal = userDetails.getUsername();
            try {
                return userService.getUserById(Long.valueOf(principal))
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build());
            } catch (NumberFormatException e) {
                return userService.getUserByUsername(principal)
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build());
            }
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final String token = authHeader.substring(7);
            String extractedUsername;
            try {
                extractedUsername = jwtTokenUtil.getUsernameFromToken(token);
            } catch (Exception e) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }

            if (extractedUsername == null || extractedUsername.trim().isEmpty()) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }

            final String username = extractedUsername;

            try {
                return userService.getUserById(Long.valueOf(username))
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build());
            } catch (NumberFormatException e) {
                return userService.getUserByUsername(username)
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build());
            }
        }

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
