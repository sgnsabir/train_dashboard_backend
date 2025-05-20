package com.banenor.controller;

import com.banenor.dto.*;
import com.banenor.security.JwtTokenUtil;
import com.banenor.service.AuthService;
import com.banenor.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

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

    /**
     * Only admins may create new users.
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody RegistrationRequest req) {

        // ------------------------------------------------
        // return 403 if not ADMIN, with correct generic
        // ------------------------------------------------
        if (currentUser == null ||
                currentUser.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .noneMatch("ROLE_ADMIN"::equals)) {
            return Mono.just(
                    ResponseEntity.<AuthResponse>status(HttpStatus.FORBIDDEN).build()
            );
        }

        // ------------------------------------------------
        // otherwise register then immediately login
        // ------------------------------------------------
        return authService.register(req)
                .flatMap(u -> authService.login(
                        new LoginRequest(u.getUsername(), req.getPassword())
                ))
                .map(token -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(token)
                );
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        return authService.login(loginRequest)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String header) {

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            return authService.logout(token)
                    .thenReturn(
                            // explicitly Void
                            ResponseEntity.<Void>ok().build()
                    );
        }

        // no token? still 200 / empty
        return Mono.just(
                ResponseEntity.<Void>ok().build()
        );
    }

    @PostMapping("/reset-password")
    public Mono<ResponseEntity<String>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {

        return authService.resetPassword(request)
                .thenReturn(ResponseEntity.ok("Password reset successful"));
    }

    @PostMapping("/change-password")
    public Mono<ResponseEntity<String>> changePassword(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody PasswordChangeRequest request) {

        if (currentUser == null) {
            log.debug("Unauthorized attempt to change password");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));
        }

        log.debug("User '{}' is attempting to change password", currentUser.getUsername());
        return authService.changePassword(request)
                .thenReturn(ResponseEntity.ok("Password changed successfully"));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String header) {

        if (header == null || !header.startsWith("Bearer ")) {
            return Mono.just(
                    ResponseEntity.<AuthResponse>status(HttpStatus.UNAUTHORIZED).build()
            );
        }

        String raw = header.substring(7);
        String username;
        try {
            username = jwtTokenUtil.getUsernameFromToken(raw);
        } catch (ExpiredJwtException e) {
            // allow refresh if only expired
            username = e.getClaims().getSubject();
        } catch (Exception ex) {
            return Mono.just(
                    ResponseEntity.<AuthResponse>status(HttpStatus.UNAUTHORIZED).build()
            );
        }

        if (username == null || username.isBlank()) {
            return Mono.just(
                    ResponseEntity.<AuthResponse>status(HttpStatus.UNAUTHORIZED).build()
            );
        }
        String trimmed = username.trim();

        return userService.findEntityByUsername(trimmed)
                .flatMap(user -> {
                    List<String> roles = user.getRole() != null
                            ? List.of("ROLE_" + user.getRole().toUpperCase())
                            : Collections.emptyList();

                    String newToken = jwtTokenUtil.generateToken(trimmed, roles);
                    long expiresIn = (jwtTokenUtil
                            .getExpirationDateFromToken(newToken)
                            .getTime() - System.currentTimeMillis()) / 1000;

                    AuthResponse resp = new AuthResponse();
                    resp.setToken(newToken);
                    resp.setUsername(trimmed);
                    resp.setExpiresIn(expiresIn);

                    return Mono.just(ResponseEntity.ok(resp));
                });
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserDTO>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String header,
            @AuthenticationPrincipal UserDetails currentUser) {

        if (currentUser != null) {
            String uname = currentUser.getUsername();
            try {
                Long id = Long.valueOf(uname);
                return userService.getUserById(id)
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(
                                ResponseEntity.<UserDTO>notFound().build()
                        );
            } catch (NumberFormatException ex) {
                return userService.getUserByUsername(uname)
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(
                                ResponseEntity.<UserDTO>notFound().build()
                        );
            }
        }

        if (header != null && header.startsWith("Bearer ")) {
            String raw = header.substring(7), uname;
            try {
                uname = jwtTokenUtil.getUsernameFromToken(raw);
            } catch (Exception e) {
                return Mono.just(
                        ResponseEntity.<UserDTO>status(HttpStatus.UNAUTHORIZED).build()
                );
            }
            if (uname == null || uname.isBlank()) {
                return Mono.just(
                        ResponseEntity.<UserDTO>status(HttpStatus.UNAUTHORIZED).build()
                );
            }
            String trimmed = uname.trim();
            try {
                Long id = Long.valueOf(trimmed);
                return userService.getUserById(id)
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(
                                ResponseEntity.<UserDTO>notFound().build()
                        );
            } catch (NumberFormatException ex) {
                return userService.getUserByUsername(trimmed)
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(
                                ResponseEntity.<UserDTO>notFound().build()
                        );
            }
        }

        return Mono.just(
                ResponseEntity.<UserDTO>status(HttpStatus.UNAUTHORIZED).build()
        );
    }
}
