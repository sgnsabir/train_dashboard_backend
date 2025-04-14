package com.banenor.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.banenor.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtRequestFilter implements WebFilter {

    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int MAX_TOKEN_LENGTH = 1024;
    private static final String ERROR_RESPONSE_TEMPLATE = "{\"error\":\"%s\",\"message\":\"%s\",\"status\":%d}";
    private static final Map<String, FailedAttempt> FAILED_ATTEMPTS = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000; // 15 minutes
    private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes

    private static class FailedAttempt {
        private final AtomicInteger count;
        private final long timestamp;

        public FailedAttempt() {
            this.count = new AtomicInteger(1);
            this.timestamp = System.currentTimeMillis();
        }

        public int incrementAndGet() {
            return count.incrementAndGet();
        }

        public int getCount() {
            return count.get();
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath().toLowerCase();
        String clientIP = extractClientIP(exchange);
        
        // Record request metrics
        meterRegistry.counter("jwt.filter.requests", 
            "path", path,
            "client_ip", maskIP(clientIP)).increment();

        // Security check for path traversal
        if (path.contains("..") || path.contains("//")) {
            meterRegistry.counter("jwt.filter.security.violations",
                "type", "path_traversal",
                "client_ip", maskIP(clientIP)).increment();
            return handleError(exchange, HttpStatus.BAD_REQUEST, "Invalid path", "Path traversal attempt detected");
        }

        // Check for IP-based rate limiting
        if (isIPLocked(clientIP)) {
            meterRegistry.counter("jwt.filter.rate.limit.exceeded",
                "client_ip", maskIP(clientIP)).increment();
            return handleError(exchange, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded", 
                "Too many failed attempts. Please try again later.");
        }

        // Bypass JWT processing for public endpoints
        if (isPublicEndpoint(path)) {
            return handlePublicEndpoint(exchange, chain);
        }

        // For non-public endpoints, check for Bearer token
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!isValidAuthHeader(authHeader)) {
            incrementFailedAttempts(clientIP);
            meterRegistry.counter("jwt.filter.auth.failures",
                "reason", "invalid_header",
                "client_ip", maskIP(clientIP)).increment();
            return handleError(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", 
                "Invalid or missing authorization header");
        }

        String jwtToken = authHeader != null ? authHeader.substring(BEARER_PREFIX.length()) : null;

        // Check if the token is blacklisted
        if (isTokenBlacklisted(jwtToken)) {
            incrementFailedAttempts(clientIP);
            meterRegistry.counter("jwt.filter.auth.failures",
                "reason", "blacklisted_token",
                "client_ip", maskIP(clientIP)).increment();
            return handleError(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", 
                "Token has been revoked");
        }

        return processToken(exchange, chain, jwtToken, clientIP);
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/v1/auth/") || 
               path.startsWith("/actuator/health") || 
               path.startsWith("/actuator/info") ||
               path.startsWith("/swagger-ui") || 
               path.startsWith("/v3/api-docs");
    }

    private Mono<Void> handlePublicEndpoint(ServerWebExchange exchange, WebFilterChain chain) {
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.headers(headers -> {
                    headers.remove(HttpHeaders.AUTHORIZATION);
                    headers.remove(HttpHeaders.COOKIE);
                }))
                .build();
        return chain.filter(mutatedExchange);
    }

    private boolean isValidAuthHeader(String authHeader) {
        return authHeader != null && 
               authHeader.startsWith(BEARER_PREFIX) && 
               authHeader.length() <= MAX_TOKEN_LENGTH;
    }

    private boolean isTokenBlacklisted(String token) {
        Cache blacklistCache = cacheManager.getCache("jwtBlacklist");
        return blacklistCache != null && blacklistCache.get(token, Boolean.class) != null;
    }

    private Mono<Void> processToken(ServerWebExchange exchange, WebFilterChain chain, 
            String jwtToken, String clientIP) {
        return Mono.defer(() -> {
            try {
                String username = jwtUtil.getUsernameFromToken(jwtToken);
                if (username == null) {
                    incrementFailedAttempts(clientIP);
                    meterRegistry.counter("jwt.filter.auth.failures",
                        "reason", "no_username",
                        "client_ip", maskIP(clientIP)).increment();
                    return handleError(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", 
                        "No username found in token");
                }

                return userDetailsService.findByUsername(username)
                    .flatMap(userDetails -> {
                        if (jwtUtil.validateToken(jwtToken, userDetails.getUsername())) {
                            // Reset failed attempts on successful authentication
                            FAILED_ATTEMPTS.remove(clientIP);
                            meterRegistry.counter("jwt.filter.auth.success",
                                "username", username,
                                "client_ip", maskIP(clientIP)).increment();
                            
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities());
                            
                            // Add security headers
                            HttpHeaders headers = exchange.getResponse().getHeaders();
                            headers.add("X-Content-Type-Options", "nosniff");
                            headers.add("X-Frame-Options", "DENY");
                            headers.add("X-XSS-Protection", "1; mode=block");
                            headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                            
                            return chain.filter(exchange)
                                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                        } else {
                            incrementFailedAttempts(clientIP);
                            meterRegistry.counter("jwt.filter.auth.failures",
                                "reason", "invalid_token",
                                "username", username,
                                "client_ip", maskIP(clientIP)).increment();
                            return handleError(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", 
                                    "Token validation failed for username: " + username);
                        }
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        incrementFailedAttempts(clientIP);
                        meterRegistry.counter("jwt.filter.auth.failures",
                            "reason", "user_not_found",
                            "username", username,
                            "client_ip", maskIP(clientIP)).increment();
                        return handleError(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", 
                                "User details not found for username: " + username);
                    }));
            } catch (Exception e) {
                incrementFailedAttempts(clientIP);
                log.error("JWT processing error: {}", e.getMessage(), e);
                meterRegistry.counter("jwt.filter.errors",
                    "error", e.getClass().getSimpleName(),
                    "client_ip", maskIP(clientIP)).increment();
                return handleError(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", 
                        "JWT token processing failed: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> handleError(ServerWebExchange exchange, HttpStatus status, 
            String error, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", error);
        responseBody.put("message", message);
        responseBody.put("status", status.value());
        responseBody.put("timestamp", Instant.now().toString());
        
        String jsonResponse;
        try {
            jsonResponse = objectMapper.writeValueAsString(responseBody);
        } catch (Exception e) {
            log.error("Error serializing error response: {}", e.getMessage());
            jsonResponse = String.format(ERROR_RESPONSE_TEMPLATE, error, message, status.value());
        }
        
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String extractClientIP(ServerWebExchange exchange) {
        String clientIP = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (clientIP != null && !clientIP.trim().isEmpty()) {
            return sanitizeIP(clientIP.split(",")[0].trim());
        }
        clientIP = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (clientIP != null && !clientIP.trim().isEmpty()) {
            return sanitizeIP(clientIP.trim());
        }
        return exchange.getRequest().getRemoteAddress() != null ? 
               sanitizeIP(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()) : 
               "unknown";
    }

    private String sanitizeIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "unknown";
        }
        // Remove any potentially harmful characters
        return ip.replaceAll("[^0-9a-fA-F:.]", "");
    }

    private void incrementFailedAttempts(String clientIP) {
        FAILED_ATTEMPTS.compute(clientIP, (ip, attempt) -> {
            if (attempt == null) {
                return new FailedAttempt();
            }
            attempt.incrementAndGet();
            return attempt;
        });
    }

    private boolean isIPLocked(String clientIP) {
        FailedAttempt attempt = FAILED_ATTEMPTS.get(clientIP);
        if (attempt != null && attempt.getCount() >= MAX_FAILED_ATTEMPTS) {
            long now = System.currentTimeMillis();
            if (now - attempt.getTimestamp() > LOCKOUT_DURATION_MS) {
                FAILED_ATTEMPTS.remove(clientIP);
                return false;
            }
            return true;
        }
        return false;
    }

    @Scheduled(fixedRate = CLEANUP_INTERVAL_MS)
    public void cleanupExpiredAttempts() {
        try {
            long now = System.currentTimeMillis();
            FAILED_ATTEMPTS.entrySet().removeIf(entry -> 
                now - entry.getValue().getTimestamp() > LOCKOUT_DURATION_MS);
            
            meterRegistry.counter("jwt.filter.cleanup.executed").increment();
        } catch (Exception e) {
            log.error("Error cleaning up expired attempts: {}", e.getMessage(), e);
            meterRegistry.counter("jwt.filter.cleanup.errors",
                "error", e.getClass().getSimpleName()).increment();
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "******";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }

    private String maskIP(String ip) {
        if (ip == null || ip.equals("unknown")) {
            return "unknown";
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return String.format("%s.%s.***.*", parts[0], parts[1]);
        }
        // Handle IPv6 addresses
        if (ip.contains(":")) {
            return ip.substring(0, ip.lastIndexOf(':')) + ":****";
        }
        return "masked";
    }
}
