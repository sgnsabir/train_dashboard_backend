package com.banenor.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:c2VjdXJlSldUU2VjcmV0S2V5MTIzNDU2Nzg5MGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6}")
    private String jwtSecret;

    @Value("${jwt.expirationSeconds:3600}")
    private Long jwtExpirationSeconds;

    private Key key;

    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
            if (keyBytes.length < 64) {
                throw new IllegalArgumentException("The JWT secret is too short. It must be at least 64 bytes (512 bits) for HS512.");
            }
            this.key = Keys.hmacShaKeyFor(keyBytes);
            log.debug("JWT key initialized successfully.");
        } catch (IllegalArgumentException e) {
            log.error("JWT key initialization failed: {}", e.getMessage());
            throw new IllegalStateException("Invalid JWT secret configuration: " + e.getMessage() +
                    ". Please provide a valid Base64-encoded string of at least 512 bits (64 bytes) for HS512.", e);
        }
    }

    public String generateToken(String username, String roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationSeconds * 1000);
        String token = Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
        log.debug("Generated token for username: {} with expiry: {}", username, expiryDate);
        return token;
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.getSubject());
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.getExpiration());
    }

    public <T> T getClaimFromToken(String token, Function<io.jsonwebtoken.Claims, T> claimsResolver) {
        final io.jsonwebtoken.Claims claims = parseToken(token);
        return claimsResolver.apply(claims);
    }

    private io.jsonwebtoken.Claims parseToken(String token) {
        if (token == null || token.trim().isEmpty() || token.split("\\.").length != 3) {
            log.error("Token format invalid: {}", token);
            throw new MalformedJwtException("JWT token does not have the expected format");
        }
        try {
            return Jwts.parserBuilder()
                    // Allow a clock skew of 60 seconds to prevent valid tokens from being rejected on refresh
                    .setAllowedClockSkewSeconds(60)
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            log.error("JWT expired: {}", ex.getMessage());
            throw ex;
        } catch (JwtException ex) {
            log.error("JWT parsing failed: {}", ex.getMessage());
            if (ex instanceof MalformedJwtException) {
                throw ex;
            }
            throw new MalformedJwtException("Invalid JWT token", ex);
        }
    }

    /**
     * Validates the token against a specific username.
     */
    public boolean validateToken(String token, String username) {
        try {
            io.jsonwebtoken.Claims claims = parseToken(token);
            String tokenUsername = claims.getSubject();
            // Use case-insensitive comparison to avoid username mismatches due to letter case
            if (tokenUsername == null || !tokenUsername.equalsIgnoreCase(username)) {
                log.error("Token username mismatch: token has '{}', expected '{}'", tokenUsername, username);
                throw new IllegalArgumentException("Username mismatch");
            }
            return true;
        } catch (ExpiredJwtException ex) {
            log.error("Token expired for username {}: {}", username, ex.getMessage());
            throw ex;
        } catch (JwtException ex) {
            log.error("Token validation error for username {}: {}", username, ex.getMessage());
            if (ex instanceof MalformedJwtException) {
                throw ex;
            }
            throw new MalformedJwtException("JWT token validation error", ex);
        }
    }

    /**
     * Overloaded method to validate the token without checking username.
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException ex) {
            log.error("JWT token validation error: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Extracts the Authentication from the token.
     * Creates a UsernamePasswordAuthenticationToken with roles parsed from the "roles" claim.
     */
    public Mono<Authentication> getAuthentication(String token) {
        try {
            io.jsonwebtoken.Claims claims = parseToken(token);
            String username = claims.getSubject();
            String rolesClaim = claims.get("roles", String.class);
            // Remove any surrounding brackets and whitespace if roles were created using List.toString()
            if (rolesClaim != null) {
                rolesClaim = rolesClaim.trim();
                if (rolesClaim.startsWith("[") && rolesClaim.endsWith("]")) {
                    rolesClaim = rolesClaim.substring(1, rolesClaim.length() - 1);
                }
            }
            List<GrantedAuthority> authorities = Arrays.stream(rolesClaim.split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            Authentication auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
            return Mono.just(auth);
        } catch (JwtException ex) {
            log.error("Error extracting authentication from token: {}", ex.getMessage());
            return Mono.error(ex);
        }
    }
}
