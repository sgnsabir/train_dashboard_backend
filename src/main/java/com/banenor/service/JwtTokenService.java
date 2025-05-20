package com.banenor.service;

import com.banenor.security.JwtTokenUtil;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JwtTokenService {

    private final JwtTokenUtil jwtTokenUtil;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;
    private final ReactiveValueOperations<String, Object> valueOps;

    // In-memory cache of token→expiry
    private static final ConcurrentHashMap<String, Date> TOKEN_EXPIRY_CACHE = new ConcurrentHashMap<>();

    public JwtTokenService(JwtTokenUtil jwtTokenUtil,
                           ReactiveRedisTemplate<String, Object> redisTemplate,
                           MeterRegistry meterRegistry) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
        this.valueOps = redisTemplate.opsForValue();
    }

    public Mono<String> generateToken(UserDetails userDetails) {
        return Mono.fromCallable(() -> {
                    List<String> roles = userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList());
                    String token = jwtTokenUtil.generateToken(userDetails.getUsername(), roles);
                    Date expiry = jwtTokenUtil.getExpirationDateFromToken(token);
                    TOKEN_EXPIRY_CACHE.put(token, expiry);
                    meterRegistry.counter("jwt.tokens.generated",
                                    "username", userDetails.getUsername(),
                                    "roles", String.join(",", roles))
                            .increment();
                    log.info("Generated token for {} expiring at {}", userDetails.getUsername(), expiry);
                    return token;
                })
                .doOnError(e -> {
                    log.error("Error generating token for {}: {}", userDetails.getUsername(), e.getMessage(), e);
                    meterRegistry.counter("jwt.tokens.generation.errors",
                            "username", userDetails.getUsername(),
                            "error", e.getClass().getSimpleName()).increment();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Boolean> validateToken(String token, UserDetails userDetails) {
        String blackKey = "jwtBlacklist:" + token;
        return valueOps.get(blackKey)
                .cast(Boolean.class)
                .defaultIfEmpty(false)
                .flatMap(blacklisted -> {
                    if (blacklisted) {
                        log.warn("Token is blacklisted: {}", mask(token));
                        meterRegistry.counter("jwt.tokens.validation.failures", "reason", "blacklisted").increment();
                        return Mono.just(false);
                    }
                    Date exp = TOKEN_EXPIRY_CACHE.get(token);
                    if (exp != null && exp.before(new Date())) {
                        log.warn("Token has expired: {}", mask(token));
                        meterRegistry.counter("jwt.tokens.validation.failures", "reason", "expired").increment();
                        return Mono.just(false);
                    }
                    return Mono.fromCallable(() -> jwtTokenUtil.validateToken(token, userDetails.getUsername()))
                            .doOnSuccess(valid -> {
                                if (valid) {
                                    meterRegistry.counter("jwt.tokens.validation.success").increment();
                                } else {
                                    meterRegistry.counter("jwt.tokens.validation.failures", "reason", "invalid").increment();
                                }
                            })
                            .doOnError(e -> {
                                log.error("Token validation error: {}", e.getMessage(), e);
                                meterRegistry.counter("jwt.tokens.validation.errors",
                                        "error", e.getClass().getSimpleName()).increment();
                            })
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }

    public Mono<String> getUsernameFromToken(String token) {
        return Mono.fromCallable(() -> jwtTokenUtil.getUsernameFromToken(token))
                .doOnSuccess(u -> {
                    if (u != null) meterRegistry.counter("jwt.tokens.username.extracted").increment();
                })
                .doOnError(e -> {
                    log.error("Username extraction failed: {}", e.getMessage(), e);
                    meterRegistry.counter("jwt.tokens.username.extraction.errors",
                            "error", e.getClass().getSimpleName()).increment();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> blacklistToken(String token) {
        return Mono.fromCallable(() -> {
                    Date expiry = jwtTokenUtil.getExpirationDateFromToken(token);
                    long millisLeft = expiry.getTime() - System.currentTimeMillis();
                    return Duration.ofMillis(Math.max(millisLeft, 0));
                })
                .flatMap(ttl -> {
                    if (ttl.isZero() || ttl.isNegative()) {
                        log.warn("Not blacklisting expired token: {}", mask(token));
                        return Mono.empty();
                    }
                    String blackKey = "jwtBlacklist:" + token;
                    return valueOps.set(blackKey, true, ttl)
                            .flatMap(ok -> {
                                if (Boolean.TRUE.equals(ok)) {
                                    meterRegistry.counter("jwt.tokens.blacklisted").increment();
                                    log.info("Blacklisted token {} for {}s", mask(token), ttl.getSeconds());
                                } else {
                                    log.warn("Failed to blacklist token {}", mask(token));
                                }
                                TOKEN_EXPIRY_CACHE.put(token, jwtTokenUtil.getExpirationDateFromToken(token));
                                return Mono.empty();
                            });
                })
                .doOnError(e -> {
                    log.error("Error blacklisting token: {}", e.getMessage(), e);
                    meterRegistry.counter("jwt.tokens.blacklist.errors",
                            "error", e.getClass().getSimpleName()).increment();
                })
                .then();
    }

    public Mono<Boolean> isTokenBlacklisted(String token) {
        String blackKey = "jwtBlacklist:" + token;
        return valueOps.get(blackKey)
                .cast(Boolean.class)
                .defaultIfEmpty(false)
                .doOnNext(b -> meterRegistry.counter("jwt.tokens.blacklist.checks",
                        "result", b ? "blacklisted" : "valid").increment())
                .doOnError(e -> {
                    log.error("Blacklist check error: {}", e.getMessage(), e);
                    meterRegistry.counter("jwt.tokens.blacklist.check.errors",
                            "error", e.getClass().getSimpleName()).increment();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Date> getTokenExpiry(String token) {
        return Mono.fromCallable(() -> {
                    Date d = TOKEN_EXPIRY_CACHE.get(token);
                    if (d == null) {
                        d = jwtTokenUtil.getExpirationDateFromToken(token);
                        TOKEN_EXPIRY_CACHE.put(token, d);
                    }
                    return d;
                })
                .doOnError(e -> {
                    log.error("Error getting token expiry: {}", e.getMessage(), e);
                    meterRegistry.counter("jwt.tokens.expiry.check.errors",
                            "error", e.getClass().getSimpleName()).increment();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> cleanupExpiredTokens() {
        return Mono.fromRunnable(() -> {
                    Instant now = Instant.now();
                    TOKEN_EXPIRY_CACHE.entrySet().removeIf(e -> e.getValue().toInstant().isBefore(now));
                    meterRegistry.counter("jwt.tokens.cleanup.executed").increment();
                    log.debug("Cleaned up expired tokens");
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private String mask(String t) {
        if (t == null || t.length() < 10) return "******";
        return t.substring(0, 6) + "..." + t.substring(t.length() - 4);
    }
}
