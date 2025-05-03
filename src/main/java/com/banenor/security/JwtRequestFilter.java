package com.banenor.security;

import com.banenor.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtRequestFilter implements WebFilter {

    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final CacheManager cacheManager;

    private static final Map<String, List<String>> roleAccessMap = Map.of(
            "/api/v1/admin", List.of("ROLE_ADMIN"),
            "/api/v1/maintenance", List.of("ROLE_ENGINEER", "ROLE_ADMIN")
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        final String path = exchange.getRequest().getPath().value().toLowerCase();

        logHeadersAndCookies(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
            log.debug("Skipping authentication for preflight OPTIONS request");
            return chain.filter(exchange);
        }

        if (isPublicPath(path)) {
            log.debug("Public path, skipping JWT processing: {}", path);
            return chain.filter(exchange);
        }

        final String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header on path: {}", path);
            return unauthorized(exchange, "Missing Bearer token");
        }

        final String token = authHeader.substring(7);
        log.debug("Token received: {}", maskToken(token));

        Cache blacklist = cacheManager.getCache("jwtBlacklist");
        if (blacklist != null && Boolean.TRUE.equals(blacklist.get(token, Boolean.class))) {
            log.warn("JWT is blacklisted: {}", maskToken(token));
            return unauthorized(exchange, "Blacklisted token");
        }

        String username;
        try {
            username = jwtTokenUtil.getUsernameFromToken(token);
        } catch (Exception e) {
            log.error("Token extraction error: {}", e.getMessage());
            return unauthorized(exchange, "Token parse failed");
        }

        if (username == null || username.isBlank()) {
            log.warn("Token missing username");
            return unauthorized(exchange, "Invalid token subject");
        }

        final String extractedUsername = username;
        return userDetailsService.findByUsername(extractedUsername)
                .flatMap(user -> {
                    if (!user.isEnabled()) {
                        log.warn("User is disabled: {}", extractedUsername);
                        return unauthorized(exchange, "Disabled user");
                    }

                    boolean isValid;
                    try {
                        isValid = jwtTokenUtil.validateToken(token, extractedUsername);
                    } catch (Exception e) {
                        log.error("Validation failed: {}", e.getMessage());
                        return unauthorized(exchange, "Token validation failed");
                    }

                    if (!isValid) {
                        log.warn("Invalid token for user: {}", extractedUsername);
                        return unauthorized(exchange, "Invalid token");
                    }

                    if (!isAuthorizedForPath(path, user.getAuthorities())) {
                        log.warn("Access denied for user {} on path {}", extractedUsername, path);
                        return forbidden(exchange, "Insufficient role for this endpoint");
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

                    log.info("Authenticated user '{}' with roles {}", extractedUsername, user.getAuthorities());
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No user found with username from token: {}", extractedUsername);
                    return unauthorized(exchange, "Unknown user");
                }))
                .onErrorResume(ex -> {
                    log.error("Unexpected error: {}", ex.getMessage(), ex);
                    return unauthorized(exchange, "Processing error");
                });
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/auth/register")
                || path.startsWith("/api/v1/auth/refresh")
                || path.startsWith("/api/v1/auth/reset-password")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/actuator/");
    }

    private boolean isAuthorizedForPath(String path, Collection<? extends GrantedAuthority> authorities) {
        return roleAccessMap.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .allMatch(entry ->
                        entry.getValue().stream()
                                .anyMatch(requiredRole ->
                                        authorities.stream().anyMatch(auth -> auth.getAuthority().equals(requiredRole))
                                )
                );
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        log.debug("Responding with 401 Unauthorized: {}", reason);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String reason) {
        log.debug("Responding with 403 Forbidden: {}", reason);
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private void logHeadersAndCookies(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        StringBuilder headerLog = new StringBuilder("Request Headers:\n");

        headers.forEach((key, value) -> {
            if (key.equalsIgnoreCase(HttpHeaders.AUTHORIZATION)) {
                headerLog.append("  ").append(key).append(": ").append(maskToken(value.get(0))).append("\n");
            } else {
                headerLog.append("  ").append(key).append(": ").append(String.join(", ", value)).append("\n");
            }
        });

        headerLog.append("Cookies:\n");
        exchange.getRequest().getCookies().forEach((name, cookies) -> {
            String cookieValue = cookies.stream()
                    .map(HttpCookie::getValue)
                    .findFirst()
                    .map(this::maskToken)
                    .orElse("N/A");
            headerLog.append("  ").append(name).append(": ").append(cookieValue).append("\n");
        });

        log.info(headerLog.toString());
    }

    private String maskToken(String token) {
        return (token != null && token.length() > 10)
                ? token.substring(0, 6) + "..." + token.substring(token.length() - 4)
                : "******";
    }
}
