package com.banenor.service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.banenor.security.JwtTokenUtil;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtTokenUtil jwtTokenUtil;
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    private static final ConcurrentHashMap<String, Date> TOKEN_EXPIRY_CACHE = new ConcurrentHashMap<>();

    public Mono<String> generateToken(UserDetails userDetails) {
        return Mono.defer(() -> {
            try {
                List<String> roles = userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList());

                String token = jwtTokenUtil.generateToken(userDetails.getUsername(), roles);
                Date expiryDate = jwtTokenUtil.getExpirationDateFromToken(token);
                TOKEN_EXPIRY_CACHE.put(token, expiryDate);

                meterRegistry.counter("jwt.tokens.generated",
                        "username", userDetails.getUsername(),
                        "roles", String.join(",", roles)).increment();

                return Mono.just(token);
            } catch (Exception e) {
                log.error("Error generating token for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
                meterRegistry.counter("jwt.tokens.generation.errors",
                        "username", userDetails.getUsername(),
                        "error", e.getClass().getSimpleName()).increment();
                return Mono.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Boolean> validateToken(String token, UserDetails userDetails) {
        return Mono.defer(() -> {
            try {
                Cache blacklistCache = cacheManager.getCache("jwtBlacklist");
                if (blacklistCache != null && Boolean.TRUE.equals(blacklistCache.get(token, Boolean.class))) {
                    log.warn("Token validation failed: Token is blacklisted");
                    meterRegistry.counter("jwt.tokens.validation.failures", "reason", "blacklisted").increment();
                    return Mono.just(false);
                }

                Date cachedExpiry = TOKEN_EXPIRY_CACHE.get(token);
                if (cachedExpiry != null && cachedExpiry.before(new Date())) {
                    log.warn("Token validation failed: Token has expired");
                    meterRegistry.counter("jwt.tokens.validation.failures", "reason", "expired").increment();
                    return Mono.just(false);
                }

                boolean isValid = jwtTokenUtil.validateToken(token, userDetails.getUsername());
                if (isValid) {
                    meterRegistry.counter("jwt.tokens.validation.success").increment();
                } else {
                    meterRegistry.counter("jwt.tokens.validation.failures", "reason", "invalid").increment();
                }

                return Mono.just(isValid);
            } catch (Exception e) {
                log.error("Error validating token: {}", e.getMessage(), e);
                meterRegistry.counter("jwt.tokens.validation.errors", "error", e.getClass().getSimpleName()).increment();
                return Mono.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<String> getUsernameFromToken(String token) {
        return Mono.defer(() -> {
            try {
                String username = jwtTokenUtil.getUsernameFromToken(token);
                if (username != null) {
                    meterRegistry.counter("jwt.tokens.username.extracted").increment();
                }
                return Mono.justOrEmpty(username);
            } catch (Exception e) {
                log.error("Error extracting username from token: {}", e.getMessage(), e);
                meterRegistry.counter("jwt.tokens.username.extraction.errors", "error", e.getClass().getSimpleName()).increment();
                return Mono.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> blacklistToken(String token) {
        return Mono.defer(() -> {
            try {
                Cache blacklistCache = cacheManager.getCache("jwtBlacklist");
                if (blacklistCache != null) {
                    Date expiryDate = jwtTokenUtil.getExpirationDateFromToken(token);
                    long ttl = expiryDate.getTime() - System.currentTimeMillis();

                    if (ttl > 0) {
                        blacklistCache.put(token, true);
                        TOKEN_EXPIRY_CACHE.put(token, expiryDate);
                        meterRegistry.counter("jwt.tokens.blacklisted").increment();
                        log.info("Token blacklisted successfully. Expires in {} seconds", ttl / 1000);
                    } else {
                        log.warn("Token already expired, not blacklisting");
                    }
                }
                return Mono.empty(); // ✅ Fixed: Return proper Mono<Void>
            } catch (Exception e) {
                log.error("Error blacklisting token: {}", e.getMessage(), e);
                meterRegistry.counter("jwt.tokens.blacklist.errors", "error", e.getClass().getSimpleName()).increment();
                return Mono.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<Boolean> isTokenBlacklisted(String token) {
        return Mono.defer(() -> {
            try {
                Cache blacklistCache = cacheManager.getCache("jwtBlacklist");
                boolean isBlacklisted = blacklistCache != null && Boolean.TRUE.equals(blacklistCache.get(token, Boolean.class));

                meterRegistry.counter("jwt.tokens.blacklist.checks", "result", isBlacklisted ? "blacklisted" : "valid").increment();
                return Mono.just(isBlacklisted);
            } catch (Exception e) {
                log.error("Error checking token blacklist status: {}", e.getMessage(), e);
                meterRegistry.counter("jwt.tokens.blacklist.check.errors", "error", e.getClass().getSimpleName()).increment();
                return Mono.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Date> getTokenExpiry(String token) {
        return Mono.defer(() -> {
            try {
                Date expiryDate = TOKEN_EXPIRY_CACHE.get(token);
                if (expiryDate == null) {
                    expiryDate = jwtTokenUtil.getExpirationDateFromToken(token);
                    TOKEN_EXPIRY_CACHE.put(token, expiryDate);
                }
                return Mono.just(expiryDate);
            } catch (Exception e) {
                log.error("Error getting token expiry: {}", e.getMessage(), e);
                meterRegistry.counter("jwt.tokens.expiry.check.errors", "error", e.getClass().getSimpleName()).increment();
                return Mono.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> cleanupExpiredTokens() {
        return Mono.defer(() -> {
            try {
                Instant now = Instant.now();
                TOKEN_EXPIRY_CACHE.entrySet().removeIf(entry -> entry.getValue().toInstant().isBefore(now));
                meterRegistry.counter("jwt.tokens.cleanup.executed").increment();
                return Mono.empty(); // ✅ Correct Mono<Void> return
            } catch (Exception e) {
                log.error("Error cleaning up expired tokens: {}", e.getMessage(), e);
                meterRegistry.counter("jwt.tokens.cleanup.errors", "error", e.getClass().getSimpleName()).increment();
                return Mono.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
