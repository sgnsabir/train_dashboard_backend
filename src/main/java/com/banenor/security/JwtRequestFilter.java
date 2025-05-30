package com.banenor.security;

import com.banenor.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtRequestFilter implements WebFilter {

    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    // Map of path prefixes to roles allowed
    private static final Map<String, List<String>> roleAccessMap = Map.of(
            "/api/v1/admin", List.of("ROLE_ADMIN"),
            "/api/v1/maintenance", List.of("ROLE_ENGINEER", "ROLE_ADMIN")
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String rawPath = exchange.getRequest().getPath().value();
        logRequestDetails(exchange);

        // Skip OPTIONS and WebSocket handshake
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())
                || "/ws/stream".equals(rawPath)) {
            log.debug("Bypassing auth for method/endpoint: {}", rawPath);
            return chain.filter(exchange);
        }

        // Public paths
        String path = rawPath.toLowerCase();
        if (isPublicPath(path)) {
            log.debug("Public path, no JWT required: {}", path);
            return chain.filter(exchange);
        }

        // Extract Bearer token
        String authHeader = exchange.getRequest().getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header on {}", path);
            return unauthorized(exchange, "Missing Bearer token");
        }
        String token = authHeader.substring(7);
        log.debug("Token received: {}", mask(token));

        // Check blacklist reactively
        String blackKey = "jwtBlacklist:" + token;
        return redisTemplate.opsForValue().get(blackKey)
                .cast(Boolean.class)
                .defaultIfEmpty(false)
                .flatMap(blacklisted -> {
                    if (blacklisted) {
                        log.warn("Blacklisted token: {}", mask(token));
                        return unauthorized(exchange, "Token blacklisted");
                    }
                    return proceedWithAuth(exchange, chain, token, path);
                });
    }

    private Mono<Void> proceedWithAuth(ServerWebExchange exchange,
                                       WebFilterChain chain,
                                       String token,
                                       String path) {
        String username;
        try {
            username = jwtTokenUtil.getUsernameFromToken(token);
        } catch (Exception e) {
            log.error("Failed to parse token: {}", e.getMessage());
            return unauthorized(exchange, "Invalid token format");
        }
        if (username == null || username.isBlank()) {
            log.warn("Token subject empty");
            return unauthorized(exchange, "Invalid token subject");
        }
        String userKey = username.trim().toLowerCase();

        return userDetailsService.findByUsername(userKey)
                .flatMap(user -> {
                    if (!user.isEnabled()) {
                        log.warn("Disabled user: {}", userKey);
                        return unauthorized(exchange, "User disabled");
                    }
                    boolean valid;
                    try {
                        valid = jwtTokenUtil.validateToken(token, userKey);
                    } catch (Exception ex) {
                        log.error("Token validation exception: {}", ex.getMessage());
                        return unauthorized(exchange, "Token validation failed");
                    }
                    if (!valid) {
                        log.warn("Invalid token for {}", userKey);
                        return unauthorized(exchange, "Invalid token");
                    }
                    if (!isAuthorized(path, user.getAuthorities())) {
                        log.warn("Access denied for {} on {}", userKey, path);
                        return forbidden(exchange, "Access denied");
                    }
                    var auth = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities()
                    );
                    log.info("Authenticated {} with {}", userKey, user.getAuthorities());
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("User not found: {}", userKey);
                    return unauthorized(exchange, "Unknown user");
                }))
                .onErrorResume(err -> {
                    log.error("Auth error: {}", err.getMessage(), err);
                    return unauthorized(exchange, "Authentication error");
                });
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/auth/refresh")
                || path.startsWith("/api/v1/auth/reset-password")
                || path.startsWith("/api/v1/auth/verify")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/actuator/");
    }

    private boolean isAuthorized(String path, Collection<? extends GrantedAuthority> auths) {
        return roleAccessMap.entrySet().stream()
                .filter(e -> path.startsWith(e.getKey()))
                .allMatch(e -> e.getValue().stream()
                        .anyMatch(role -> auths.stream()
                                .anyMatch(a -> a.getAuthority().equals(role))
                        )
                );
    }

    private Mono<Void> unauthorized(ServerWebExchange ex, String reason) {
        log.debug("401 Unauthorized: {}", reason);
        ex.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return ex.getResponse().setComplete();
    }

    private Mono<Void> forbidden(ServerWebExchange ex, String reason) {
        log.debug("403 Forbidden: {}", reason);
        ex.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return ex.getResponse().setComplete();
    }

    private void logRequestDetails(ServerWebExchange ex) {
        var sb = new StringBuilder("Request ")
                .append(ex.getRequest().getMethod())
                .append(" ")
                .append(ex.getRequest().getURI())
                .append("\nHeaders:\n");
        ex.getRequest().getHeaders().forEach((k, v) ->
                sb.append("  ")
                        .append(k)
                        .append(": ")
                        .append(HttpHeaders.AUTHORIZATION.equalsIgnoreCase(k)
                                ? mask(v.get(0))
                                : String.join(",", v))
                        .append("\n")
        );
        sb.append("Cookies:\n");
        ex.getRequest().getCookies().forEach((n, c) ->
                sb.append("  ").append(n).append(": ")
                        .append(c.stream().findFirst()
                                .map(HttpCookie::getValue)
                                .map(this::mask)
                                .orElse("n/a"))
                        .append("\n")
        );
        log.info(sb.toString());
    }

    private String mask(String t) {
        if (t == null || t.length() < 10) return "******";
        return t.substring(0, 6) + "..." + t.substring(t.length() - 4);
    }
}
