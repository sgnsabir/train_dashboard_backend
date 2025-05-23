package com.banenor.controller;

import com.banenor.dto.*;
import com.banenor.security.JwtTokenUtil;
import com.banenor.service.AuthService;
import com.banenor.service.EmailService;
import com.banenor.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and account management")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenUtil jwtTokenUtil;
    private final EmailService emailService;
    private final UserService userService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Void>> register(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody RegistrationRequest req
    ) {
        if (currentUser == null ||
                currentUser.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .noneMatch("ROLE_ADMIN"::equals)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }

        return authService.register(req)
                .flatMap(user -> {
                    String verificationToken = jwtTokenUtil.generateToken(user.getUsername(), List.of());
                    String link = emailService.buildVerificationLink(verificationToken);
                    return emailService.sendVerificationEmail(user.getEmail(), link);
                })
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());
    }

    @GetMapping("/verify")
    public Mono<ResponseEntity<String>> verifyEmail(@RequestParam("token") String token) {
        try {
            jwtTokenUtil.getUsernameFromToken(token);
        } catch (ExpiredJwtException ex) {
            // allow expired for verification
        } catch (Exception ex) {
            log.warn("Invalid verification token: {}", ex.getMessage());
            return Mono.just(ResponseEntity.badRequest().body("Invalid verification token"));
        }

        return authService.verifyToken(token)
                .thenReturn(ResponseEntity.ok("Email successfully verified. You can now log in."));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        return authService.login(loginRequest)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String header
    ) {
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            return authService.logout(token)
                    .thenReturn(ResponseEntity.ok().<Void>build());
        }
        return Mono.just(ResponseEntity.ok().<Void>build());
    }

    @PostMapping("/reset-password")
    public Mono<ResponseEntity<String>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        return authService.resetPassword(request)
                .thenReturn(ResponseEntity.ok("Password reset successful"));
    }

    @PostMapping("/change-password")
    public Mono<ResponseEntity<String>> changePassword(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        if (currentUser == null) {
            log.debug("Unauthorized change-password attempt");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));
        }
        log.debug("User '{}' changing password", currentUser.getUsername());
        return authService.changePassword(request)
                .thenReturn(ResponseEntity.ok("Password changed successfully"));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String header
    ) {
        if (header == null || !header.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String raw = header.substring(7);
        String userName;
        try {
            userName = jwtTokenUtil.getUsernameFromToken(raw);
        } catch (ExpiredJwtException e) {
            userName = e.getClaims().getSubject();
        } catch (Exception ex) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        if (userName == null || userName.isBlank()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String trimmed = userName.trim();

        return userService.findEntityByUsername(trimmed)
                .flatMap(user -> {
                    List<String> roles = user.getRole() != null
                            ? List.of("ROLE_" + user.getRole().toUpperCase())
                            : List.of();
                    String newToken = jwtTokenUtil.generateToken(trimmed, roles);
                    long expiresIn = (jwtTokenUtil.getExpirationDateFromToken(newToken).getTime()
                            - System.currentTimeMillis()) / 1000;
                    AuthResponse resp = new AuthResponse();
                    resp.setToken(newToken);
                    resp.setUsername(trimmed);
                    resp.setExpiresIn(expiresIn);
                    resp.setRoles(roles);
                    return Mono.just(ResponseEntity.ok(resp));
                });
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserDTO>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String header,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        if (currentUser != null) {
            return userService.getUserByUsername(currentUser.getUsername())
                    .map(ResponseEntity::ok)
                    .defaultIfEmpty(ResponseEntity.notFound().build());
        }
        if (header != null && header.startsWith("Bearer ")) {
            String raw = header.substring(7), uname;
            try {
                uname = jwtTokenUtil.getUsernameFromToken(raw);
            } catch (Exception e) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }
            return userService.getUserByUsername(uname.trim())
                    .map(ResponseEntity::ok)
                    .defaultIfEmpty(ResponseEntity.notFound().build());
        }
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
