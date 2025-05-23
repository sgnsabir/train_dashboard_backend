package com.banenor.service;

import com.banenor.audit.Audit;
import com.banenor.dto.*;
import com.banenor.exception.InvalidCredentialsException;
import com.banenor.exception.UserAlreadyExistsException;
import com.banenor.exception.UserNotFoundException;
import com.banenor.model.User;
import com.banenor.repository.UserRepository;
import com.banenor.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ReactiveAuthenticationManager authenticationManager;
    private final ReactiveUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final EmailService emailService;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    @Override
    @Audit(action = "User Registration", resource = "User")
    public Mono<User> register(RegistrationRequest req) {
        String username = req.getUsername().trim();
        String email    = req.getEmail().trim();
        String rawPwd   = req.getPassword().trim();

        return userRepository.findByUsername(username)
                .flatMap(u -> Mono.<User>error(new UserAlreadyExistsException("Username is already taken")))
                .switchIfEmpty(
                        userRepository.findByEmail(email)
                                .flatMap(u -> Mono.<User>error(new UserAlreadyExistsException("Email is already registered")))
                                .switchIfEmpty(Mono.defer(() -> {
                                    User user = User.builder()
                                            .username(username)
                                            .email(email)
                                            .password(passwordEncoder.encode(rawPwd))
                                            .role("USER")
                                            .enabled(false)
                                            .build();

                                    log.info("Registering new user (disabled until verification): {}", username);

                                    return userRepository.save(user)
                                            .flatMap(saved -> {
                                                String token = jwtTokenUtil.generateToken(saved.getUsername(), Collections.emptyList());
                                                String link  = emailService.buildVerificationLink(token);
                                                return emailService.sendVerificationEmail(saved.getEmail(), link)
                                                        .thenReturn(saved);
                                            })
                                            .onErrorMap(DataIntegrityViolationException.class,
                                                    ex -> new UserAlreadyExistsException("Username or email already exists", ex));
                                }))
                );
    }

    @Override
    @Audit(action = "User Login", resource = "Authentication")
    public Mono<AuthResponse> login(LoginRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String password = req.getPassword() == null ? "" : req.getPassword().trim();

        if (username.isEmpty() || password.isEmpty()) {
            return Mono.error(new InvalidCredentialsException("Username and password are required"));
        }

        return authenticationManager
                .authenticate(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(username, password))
                .onErrorMap(DisabledException.class,
                        ex -> new InvalidCredentialsException("Account not yet verified", ex))
                .onErrorMap(BadCredentialsException.class,
                        ex -> new InvalidCredentialsException("Incorrect username or password", ex))
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Incorrect username or password")))
                .then(userDetailsService.findByUsername(username))
                .flatMap(ud -> {
                    List<String> roles = ud.getAuthorities().stream()
                            .map(a -> a.getAuthority())
                            .collect(Collectors.toList());

                    String token = jwtTokenUtil.generateToken(ud.getUsername(), roles);
                    Date exp     = jwtTokenUtil.getExpirationDateFromToken(token);
                    long expiresIn = (exp.getTime() - System.currentTimeMillis()) / 1000;

                    AuthResponse resp = new AuthResponse();
                    resp.setToken(token);
                    resp.setUsername(ud.getUsername());
                    resp.setExpiresIn(expiresIn);
                    resp.setRoles(roles);
                    return Mono.just(resp);
                });
    }

    @Override
    @Audit(action = "User Logout", resource = "Authentication")
    public Mono<Void> logout(String token) {
        if (token == null || token.isBlank()) {
            return Mono.empty();
        }
        log.debug("Blacklisting JWT until expiry");
        Date exp = jwtTokenUtil.getExpirationDateFromToken(token);
        long ttlSeconds = Math.max(0, (exp.getTime() - System.currentTimeMillis()) / 1000);
        String key = "jwtBlacklist:" + token;
        return redisTemplate.opsForValue()
                .set(key, Boolean.TRUE, Duration.ofSeconds(ttlSeconds))
                .then();
    }

    @Override
    @Audit(action = "Password Reset", resource = "User")
    public Mono<Void> resetPassword(PasswordResetRequest req) {
        String email = req.getEmail().trim();
        String newPwd = req.getNewPassword().trim();
        // assume you’ve validated req.getToken() elsewhere

        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new UserNotFoundException("No user with email: " + email)))
                .flatMap(user -> {
                    user.setPassword(passwordEncoder.encode(newPwd));
                    log.info("Reset password for {}", user.getUsername());
                    return userRepository.save(user).then();
                });
    }

    @Override
    @Audit(action = "Change Password", resource = "User")
    public Mono<Void> changePassword(PasswordChangeRequest req) {
        String oldPwd = req.getOldPassword().trim();
        String newPwd = req.getNewPassword().trim();

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> userRepository.findByUsername(username)
                        .switchIfEmpty(Mono.error(new UserNotFoundException("User not found: " + username)))
                        .flatMap(user -> {
                            if (!passwordEncoder.matches(oldPwd, user.getPassword())) {
                                return Mono.error(new InvalidCredentialsException("Current password is incorrect"));
                            }
                            user.setPassword(passwordEncoder.encode(newPwd));
                            log.info("User {} changed their password", username);
                            return userRepository.save(user).then();
                        })
                );
    }

    @Override
    @Audit(action = "Email Verification", resource = "User")
    public Mono<Void> verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return Mono.error(new InvalidCredentialsException("Verification token is required"));
        }

        return Mono.defer(() -> {
            if (!jwtTokenUtil.validateToken(token)) {
                return Mono.error(new InvalidCredentialsException("Invalid or expired verification token"));
            }
            String username = jwtTokenUtil.getUsernameFromToken(token);
            return userRepository.findByUsername(username)
                    .switchIfEmpty(Mono.error(new UserNotFoundException("User not found: " + username)))
                    .flatMap(user -> {
                        if (user.isEnabled()) {
                            log.info("User {} already verified", username);
                            return Mono.empty();
                        }
                        user.setEnabled(true);
                        log.info("User {} has been verified and enabled", username);
                        return userRepository.save(user).then();
                    });
        });
    }
}
