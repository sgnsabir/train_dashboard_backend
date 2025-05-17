package com.banenor.security;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenUtil {

    /**
     * Base64-encoded secret. If unset, we auto-generate a fresh 256-bit key.
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Token TTL in seconds.
     */
    @Value("${jwt.expiration-in-seconds:3600}")
    private Long jwtExpirationSeconds;

    /**
     * The JJWT signing key derived from the Base64 secret.
     */
    private Key signingKey;

    @PostConstruct
    public void init() {
        // If no secret provided, generate a new 256‐bit HmacSHA256 key:
        if (jwtSecret == null || jwtSecret.isBlank()) {
            jwtSecret = generateSecretKey();
            log.info("🔐 Generated new JWT secret (persist this!): {}", jwtSecret);
        }

        // Decode once and build JJWT signing key:
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must decode to at least 256 bits (32 bytes)");
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private String generateSecretKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("HmacSHA256");
            kg.init(256);
            SecretKey sk = kg.generateKey();
            return Base64.getEncoder().encodeToString(sk.getEncoded());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate JWT secret key", ex);
        }
    }

    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date expires = new Date(now.getTime() + jwtExpirationSeconds * 1000);

        boolean isAdmin = roles.stream().anyMatch(r -> r.equalsIgnoreCase("ROLE_ADMIN"));

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expires)
                .claim("isAdmin", isAdmin)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaim(token, io.jsonwebtoken.Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaim(token, io.jsonwebtoken.Claims::getExpiration);
    }

    public <T> T getClaim(String token, Function<io.jsonwebtoken.Claims, T> resolver) {
        io.jsonwebtoken.Claims claims = parse(token);
        return resolver.apply(claims);
    }

    private io.jsonwebtoken.Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .setAllowedClockSkewSeconds(60)  // allow 1m clock skew
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Basic validity check: can we parse it at all?
     */
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException ex) {
            log.warn("Invalid JWT: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Also confirm the subject matches.
     */
    public boolean validateToken(String token, String expectedUsername) {
        try {
            String actual = getUsernameFromToken(token);
            return actual != null && actual.equalsIgnoreCase(expectedUsername);
        } catch (JwtException ex) {
            log.warn("JWT validation error: {}", ex.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Mono<Authentication> getAuthentication(String token) {
        try {
            var claims = parse(token);
            String username = claims.getSubject();

            Object rawRoles = claims.get("roles");
            List<String> roles = (rawRoles instanceof List<?>)
                    ? ((List<?>) rawRoles).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList())
                    : List.of();

            var authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            return Mono.just(
                    new UsernamePasswordAuthenticationToken(username, null, authorities)
            );
        } catch (Exception ex) {
            log.error("Failed to extract Authentication from token", ex);
            return Mono.error(ex);
        }
    }
}

