package com.banenor.controller;

import com.banenor.dto.*;
import com.banenor.model.User;
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

    @PostMapping("/register")
    public Mono<ResponseEntity<LoginResponse>> register(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody RegistrationRequest req) {

        if (currentUser == null ||
                currentUser.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .noneMatch("ROLE_ADMIN"::equals)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }

        return authService.register(req)
                .flatMap(u -> authService.login(
                        new LoginRequest(u.getUsername(), req.getPassword())))
                .map(token -> ResponseEntity.status(HttpStatus.CREATED).body(token));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String h) {
        if (h != null && h.startsWith("Bearer ")) {
            return authService.logout(h.substring(7))
                    .thenReturn(ResponseEntity.ok().<Void>build());
        }
        return Mono.just(ResponseEntity.ok().<Void>build());
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
            @Valid @RequestBody ChangePasswordRequest request) {

        if (currentUser == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized"));
        }
        return authService.changePassword(
                        currentUser.getUsername(),
                        request.getOldPassword(),
                        request.getNewPassword())
                .thenReturn(ResponseEntity.ok("Password changed successfully"));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<LoginResponse>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String h) {

        if (h == null || !h.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String raw = h.substring(7);
        String username;
        try {
            username = jwtTokenUtil.getUsernameFromToken(raw);
        } catch (ExpiredJwtException e) {
            username = e.getClaims().getSubject();
        } catch (Exception ex) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        if (username == null || username.isBlank()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        String trimmed = username.trim();

        return userService.findEntityByUsername(trimmed)
                .flatMap(user -> {
                    List<String> roles = user.getRole()!=null
                            ? List.of("ROLE_" + user.getRole().toUpperCase())
                            : Collections.emptyList();

                    String newToken = jwtTokenUtil.generateToken(trimmed, roles);
                    long expiresIn = (jwtTokenUtil
                            .getExpirationDateFromToken(newToken)
                            .getTime() - System.currentTimeMillis()) / 1000;

                    LoginResponse resp = new LoginResponse();
                    resp.setToken(newToken);
                    resp.setUsername(trimmed);
                    resp.setExpiresIn(expiresIn);
                    return Mono.just(ResponseEntity.ok(resp));
                });
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserResponse>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String h,
            @AuthenticationPrincipal UserDetails currentUser) {

        if (currentUser != null) {
            String p = currentUser.getUsername();
            try {
                Long id = Long.valueOf(p);
                return userService.getUserById(id)
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build());
            } catch (NumberFormatException ex) {
                return userService.getUserByUsername(p)
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build());
            }
        }

        if (h != null && h.startsWith("Bearer ")) {
            String raw = h.substring(7), u;
            try {
                u = jwtTokenUtil.getUsernameFromToken(raw);
            } catch (Exception e) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }
            if (u == null || u.isBlank()) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }
            String t = u.trim();
            try {
                Long id = Long.valueOf(t);
                return userService.getUserById(id)
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build());
            } catch (NumberFormatException ex) {
                return userService.getUserByUsername(t)
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build());
            }
        }

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
