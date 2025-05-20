package com.banenor.service;

import com.banenor.audit.Audit;
import com.banenor.dto.*;
import com.banenor.exception.InvalidCredentialsException;
import com.banenor.exception.UserAlreadyExistsException;
import com.banenor.exception.UserNotFoundException;
import com.banenor.model.User;
import com.banenor.repository.UserRepository;
import com.banenor.security.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final ReactiveAuthenticationManager authenticationManager;
    private final ReactiveUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthServiceImpl(ReactiveAuthenticationManager authenticationManager,
                           ReactiveUserDetailsService userDetailsService,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenUtil jwtTokenUtil) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    @Audit(action = "User Registration", resource = "User")
    public Mono<User> register(RegistrationRequest req) {
        return userRepository.findByUsername(req.getUsername())
                .flatMap(u -> Mono.<User>error(new UserAlreadyExistsException("Username is already taken")))
                .switchIfEmpty(
                        userRepository.findByEmail(req.getEmail())
                                .flatMap(u -> Mono.<User>error(new UserAlreadyExistsException("Email is already registered")))
                                .switchIfEmpty(Mono.defer(() -> {
                                    User user = User.builder()
                                            .username(req.getUsername())
                                            .email(req.getEmail())
                                            .password(passwordEncoder.encode(req.getPassword()))
                                            .role("USER")
                                            .build();
                                    log.info("Registering new user: {}", req.getUsername());
                                    return userRepository.save(user);
                                }))
                );
    }

    @Override
    @Audit(action = "User Login", resource = "Authentication")
    public Mono<AuthResponse> login(LoginRequest req) {
        if (req.getUsername() == null || req.getUsername().trim().isEmpty()
                || req.getPassword() == null || req.getPassword().trim().isEmpty()) {
            return Mono.error(new InvalidCredentialsException("Username and password are required"));
        }
        String username = req.getUsername().trim();
        String password = req.getPassword().trim();

        return authenticationManager.authenticate(
                        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(username, password)
                )
                .onErrorMap(BadCredentialsException.class,
                        ex -> new InvalidCredentialsException("Incorrect username or password", ex))
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Incorrect username or password")))
                .map(auth -> username)
                .flatMap(userDetailsService::findByUsername)
                .map(ud -> {
                    List<String> roles = ud.getAuthorities().stream()
                            .map(a -> a.getAuthority())
                            .collect(Collectors.toList());
                    String token = jwtTokenUtil.generateToken(ud.getUsername(), roles);
                    Date exp = jwtTokenUtil.getExpirationDateFromToken(token);
                    long expiresIn = (exp.getTime() - System.currentTimeMillis()) / 1000;
                    AuthResponse resp = new AuthResponse();
                    resp.setToken(token);
                    resp.setUsername(ud.getUsername());
                    resp.setExpiresIn(expiresIn);
                    resp.setRoles(roles);
                    return resp;
                });
    }

    @Override
    @Audit(action = "User Logout", resource = "Authentication")
    public Mono<Void> logout(String token) {
        log.debug("Logging out token: {}", token);
        // TODO: blacklist token if needed
        return Mono.empty();
    }

    @Override
    @Audit(action = "Password Reset", resource = "User")
    public Mono<Void> resetPassword(PasswordResetRequest req) {
        Objects.requireNonNull(req.getEmail(), "Email must be provided");
        Objects.requireNonNull(req.getToken(), "Reset token must be provided");
        Objects.requireNonNull(req.getNewPassword(), "New password must be provided");

        // TODO: validate req.getToken() against your reset-token store

        return userRepository.findByEmail(req.getEmail())
                .switchIfEmpty(Mono.error(new UserNotFoundException(
                        "User not found with email: " + req.getEmail())))
                .flatMap(user -> {
                    user.setPassword(passwordEncoder.encode(req.getNewPassword()));
                    log.debug("Reset password for user {}", user.getUsername());
                    return userRepository.save(user).then();
                });
    }

    @Override
    @Audit(action = "Change Password", resource = "User")
    public Mono<Void> changePassword(PasswordChangeRequest req) {
        Objects.requireNonNull(req.getOldPassword(), "Old password must be provided");
        Objects.requireNonNull(req.getNewPassword(), "New password must be provided");

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username ->
                        userRepository.findByUsername(username)
                                .switchIfEmpty(Mono.error(new UserNotFoundException(
                                        "User not found with username: " + username)))
                                .flatMap(user -> {
                                    if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
                                        return Mono.error(new BadCredentialsException("Incorrect current password"));
                                    }
                                    user.setPassword(passwordEncoder.encode(req.getNewPassword()));
                                    log.debug("Changed password for user {}", username);
                                    return userRepository.save(user).then();
                                })
                );
    }
}
