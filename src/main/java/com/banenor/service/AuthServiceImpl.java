package com.banenor.service;

import com.banenor.audit.Audit;
import com.banenor.dto.LoginRequest;
import com.banenor.dto.LoginResponse;
import com.banenor.dto.PasswordResetRequest;
import com.banenor.dto.RegistrationRequest;
import com.banenor.exception.InvalidCredentialsException;
import com.banenor.exception.UserAlreadyExistsException;
import com.banenor.exception.UserNotFoundException;
import com.banenor.model.User;
import com.banenor.repository.UserRepository;
import com.banenor.security.JwtTokenUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;
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
    public Mono<User> register(RegistrationRequest registrationRequest) {
        return userRepository.findByUsername(registrationRequest.getUsername())
                .flatMap(existingUser -> Mono.<User>error(new UserAlreadyExistsException("Username is already taken")))
                .switchIfEmpty(
                        userRepository.findByEmail(registrationRequest.getEmail())
                                .flatMap(existingEmail -> Mono.<User>error(new UserAlreadyExistsException("Email is already registered")))
                                .switchIfEmpty(Mono.defer(() -> {
                                    User user = User.builder()
                                            .username(registrationRequest.getUsername())
                                            .email(registrationRequest.getEmail())
                                            .password(passwordEncoder.encode(registrationRequest.getPassword()))
                                            .role("USER")
                                            .build();
                                    log.info("Registering new user: {}", registrationRequest.getUsername());
                                    return userRepository.save(user);
                                }))
                );
    }

    @Override
    @Audit(action = "User Login", resource = "Authentication")
    public Mono<LoginResponse> login(LoginRequest loginRequest) {
        return authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()))
                .onErrorMap(BadCredentialsException.class,
                        ex -> new InvalidCredentialsException("Incorrect username or password", ex))
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Incorrect username or password")))
                .map(auth -> loginRequest.getUsername())
                .flatMap(username ->
                        userDetailsService.findByUsername(username)
                                .map(userDetails -> {
                                    List<String> roles = userDetails.getAuthorities().stream()
                                            .map(grantedAuthority -> grantedAuthority.getAuthority())
                                            .collect(Collectors.toList());

                                    String token = jwtTokenUtil.generateToken(userDetails.getUsername(), roles);

                                    Date expirationDate = jwtTokenUtil.getExpirationDateFromToken(token);
                                    long expiresIn = (expirationDate.getTime() - System.currentTimeMillis()) / 1000;

                                    LoginResponse response = new LoginResponse();
                                    response.setToken(token);
                                    response.setUsername(userDetails.getUsername());
                                    response.setExpiresIn(expiresIn);
                                    return response;
                                })
                );
    }

    @Override
    @Audit(action = "User Logout", resource = "Authentication")
    public Mono<Void> logout(String token) {
        log.info("Logging out token: {}", token);
        // Token blacklisting or session management can be added here.
        return Mono.empty();
    }

    @Override
    @Audit(action = "Password Reset", resource = "User")
    public Mono<Void> resetPassword(PasswordResetRequest passwordResetRequest) {
        return userRepository.findByEmail(passwordResetRequest.getEmail())
                .switchIfEmpty(Mono.<User>error(new UserNotFoundException("User not found with email: "
                        + passwordResetRequest.getEmail())))
                .flatMap(user -> {
                    user.setPassword(passwordEncoder.encode(passwordResetRequest.getNewPassword()));
                    log.info("Resetting password for user: {}", user.getUsername());
                    return userRepository.save(user).then();
                });
    }

    @Override
    @Audit(action = "Change Password", resource = "User")
    public Mono<Void> changePassword(String username, String oldPassword, String newPassword) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with username: " + username)))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                        return Mono.error(new BadCredentialsException("Incorrect current password"));
                    }
                    user.setPassword(passwordEncoder.encode(newPassword));
                    log.info("Changing password for user: {}", username);
                    return userRepository.save(user).then();
                });
    }
}
