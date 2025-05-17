package com.banenor.service;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.banenor.model.User;
import com.banenor.repository.UserRepository;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;
    private final MeterRegistry meterRegistry;

    private static final ConcurrentHashMap<String, Integer> FAILED_LOGIN_ATTEMPTS = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        // EARLY GUARD: reject null or blank usernames immediately
        if (username == null || username.trim().isEmpty()) {
            return Mono.error(new UsernameNotFoundException("Username must be provided"));
        }

        String normalized = username.trim().toLowerCase();

        return Mono.defer(() -> {
                    // Check for account lockout
                    if (isAccountLocked(normalized)) {
                        log.warn("Account locked for user: {}", normalized);
                        meterRegistry.counter("user.login.attempts.locked",
                                "username", normalized).increment();
                        return Mono.error(new UsernameNotFoundException(
                                "Account is temporarily locked. Please try again later."));
                    }

                    return userRepository.findByUsername(normalized)
                            .switchIfEmpty(Mono.defer(() -> {
                                incrementFailedAttempts(normalized);
                                meterRegistry.counter("user.login.attempts.failed",
                                        "username", normalized,
                                        "reason", "not_found").increment();
                                return Mono.error(new UsernameNotFoundException(
                                        "User not found with username: " + normalized));
                            }))
                            .map(user -> {
                                // Reset failed attempts on successful lookup
                                FAILED_LOGIN_ATTEMPTS.remove(normalized);
                                meterRegistry.counter("user.login.attempts.success",
                                        "username", normalized).increment();

                                return createUserDetails(user);
                            })
                            .doOnError(e -> {
                                if (!(e instanceof UsernameNotFoundException)) {
                                    log.error("Error finding user {}: {}", normalized, e.getMessage(), e);
                                    meterRegistry.counter("user.login.attempts.error",
                                            "username", normalized,
                                            "error", e.getClass().getSimpleName()).increment();
                                }
                            });
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private UserDetails createUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true,  // accountNonExpired
                true,  // credentialsNonExpired
                !user.isLocked(), // accountNonLocked
                Collections.singletonList(() -> "ROLE_" + user.getRole().toUpperCase())
        );
    }

    private void incrementFailedAttempts(String username) {
        FAILED_LOGIN_ATTEMPTS.compute(username, (key, attempts) ->
                (attempts == null) ? 1 : attempts + 1
        );
    }

    private boolean isAccountLocked(String username) {
        Integer attempts = FAILED_LOGIN_ATTEMPTS.get(username);
        return attempts != null && attempts >= MAX_FAILED_ATTEMPTS;
    }

    public Mono<Void> resetFailedAttempts(String username) {
        return Mono.defer(() -> {
            try {
                FAILED_LOGIN_ATTEMPTS.remove(username);
                meterRegistry.counter("user.login.attempts.reset",
                        "username", username).increment();
                return Mono.empty().then();
            } catch (Exception e) {
                log.error("Error resetting failed attempts for user {}: {}",
                        username, e.getMessage(), e);
                meterRegistry.counter("user.login.attempts.reset.error",
                        "username", username,
                        "error", e.getClass().getSimpleName()).increment();
                return Mono.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Integer> getFailedAttempts(String username) {
        return Mono.defer(() -> {
            try {
                Integer attempts = FAILED_LOGIN_ATTEMPTS.get(username);
                return Mono.just(attempts != null ? attempts : 0);
            } catch (Exception e) {
                log.error("Error getting failed attempts for user {}: {}",
                        username, e.getMessage(), e);
                meterRegistry.counter("user.login.attempts.get.error",
                        "username", username,
                        "error", e.getClass().getSimpleName()).increment();
                return Mono.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> cleanupExpiredLockouts() {
        return Mono.defer(() -> {
            try {
                long now = System.currentTimeMillis();
                FAILED_LOGIN_ATTEMPTS.entrySet().removeIf(entry ->
                        entry.getValue() >= MAX_FAILED_ATTEMPTS);

                meterRegistry.counter("user.login.attempts.cleanup.executed").increment();
                return Mono.empty().then();
            } catch (Exception e) {
                log.error("Error cleaning up expired lockouts: {}", e.getMessage(), e);
                meterRegistry.counter("user.login.attempts.cleanup.error",
                        "error", e.getClass().getSimpleName()).increment();
                return Mono.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
