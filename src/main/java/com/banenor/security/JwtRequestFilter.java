package com.banenor.security;

import com.banenor.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtRequestFilter implements WebFilter {

    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final CacheManager cacheManager;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Bypass JWT processing for public endpoints.
        String requestPath = exchange.getRequest().getURI().getPath();
        if (requestPath.equalsIgnoreCase("/api/v1/auth/login")
                || requestPath.equalsIgnoreCase("/api/v1/auth/register")
                || requestPath.startsWith("/actuator/")) {
            log.debug("Bypassing JWT filter for public endpoint: {}", requestPath);
            return chain.filter(exchange);
        }

        String path = requestPath.toLowerCase();
        if (path.startsWith("/api/v1/auth/")) {
            // Bypass additional auth endpoints by removing cookies.
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r.headers(httpHeaders -> httpHeaders.remove(HttpHeaders.COOKIE)))
                    .build();
            log.debug("Bypassing JWT filter for auth endpoint: {}", path);
            return chain.filter(mutatedExchange);
        }

        // Check for Authorization header; if missing or not Bearer, reject the request.
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token provided. Rejecting request.");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String jwtToken = authHeader.substring(7);
        log.debug("Bearer token found: {}", maskToken(jwtToken));

        // Check if token is blacklisted.
        Cache blacklistCache = cacheManager.getCache("jwtBlacklist");
        if (blacklistCache != null && blacklistCache.get(jwtToken, Boolean.class) != null) {
            log.warn("JWT token is blacklisted: {}", maskToken(jwtToken));
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Extract username and ensure it is final/effectively final.
        String extractedUsername = jwtUtil.getUsernameFromToken(jwtToken);
        log.debug("Extracted username from token: {}", extractedUsername);
        final String username = (extractedUsername != null) ? extractedUsername.trim().toLowerCase() : "";
        if (username.isEmpty()) {
            log.error("No username found in token.");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return userDetailsService.findByUsername(username)
                .flatMap((UserDetails userDetails) -> {
                    if (!userDetails.isEnabled()) {
                        log.error("User {} is disabled", username);
                        return unauthorizedResponse(exchange);
                    }
                    if (jwtUtil.validateToken(jwtToken, userDetails.getUsername())) {
                        log.debug("Token validated successfully for username: {}", username);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                    } else {
                        log.error("Token validation failed for username: {}", username);
                        return unauthorizedResponse(exchange);
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("User details not found for username: {}", username);
                    return unauthorizedResponse(exchange);
                }))
                .onErrorResume(e -> {
                    log.error("JWT parse/validate error: {}", e.getMessage(), e);
                    return unauthorizedResponse(exchange);
                });
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "******";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}
