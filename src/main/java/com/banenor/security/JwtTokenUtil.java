package com.banenor.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;  // injected from application.properties or $JWT_SECRET

    @Value("${jwt.expiration-in-seconds}")
    private Long jwtExpirationSeconds;  // injected from application.properties or $JWT_EXPIRATION_IN_SECONDS

    private Key signingKey;

    @PostConstruct
    public void init() {
        // Base64-decode once
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException(
                    "JWT secret must decode to at least 512 bits (64 bytes)");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date expires = new Date(now.getTime() + jwtExpirationSeconds * 1000);
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expires)
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaim(token, io.jsonwebtoken.Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaim(token, io.jsonwebtoken.Claims::getExpiration);
    }

    public <T> T getClaim(String token, Function<io.jsonwebtoken.Claims, T> resolver) {
        var claims = parse(token);
        return resolver.apply(claims);
    }

    private io.jsonwebtoken.Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .setAllowedClockSkewSeconds(60)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token, String expectedUsername) {
        try {
            String actual = getUsernameFromToken(token);
            return actual != null && actual.equalsIgnoreCase(expectedUsername);
        } catch (JwtException ex) {
            log.warn("JWT validation error: {}", ex.getMessage());
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException ex) {
            log.warn("Invalid JWT: {}", ex.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Mono<Authentication> getAuthentication(String token) {
        try {
            var claims   = parse(token);
            String username = claims.getSubject();
            Object raw      = claims.get("roles");
            List<String> roles = (raw instanceof List<?>)
                    ? ((List<?>) raw).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList())
                    : List.of();
            var authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            return Mono.just(
                    new UsernamePasswordAuthenticationToken(username, null, authorities));
        } catch (Exception ex) {
            log.error("Failed to extract Authentication from token", ex);
            return Mono.error(ex);
        }
    }
}
