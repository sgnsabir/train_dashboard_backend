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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-in-seconds}")
    private Long jwtExpirationSeconds;

    private Key key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException("JWT secret must be at least 512 bits (64 bytes)");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationSeconds * 1000);
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
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
        return Jwts.parserBuilder()
                .setAllowedClockSkewSeconds(60)
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token, String username) {
        try {
            String tokenUsername = getUsernameFromToken(token);
            return tokenUsername != null && tokenUsername.equalsIgnoreCase(username);
        } catch (JwtException ex) {
            log.error("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException ex) {
            log.error("Invalid JWT: {}", ex.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Mono<Authentication> getAuthentication(String token) {
        try {
            var claims = parseToken(token);
            String username = claims.getSubject();

            Object rolesObject = claims.get("roles");

            List<String> roles;
            if (rolesObject instanceof List<?>) {
                roles = ((List<?>) rolesObject).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
            } else {
                roles = List.of(); // fallback
            }

            List<GrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            return Mono.just(new UsernamePasswordAuthenticationToken(username, null, authorities));
        } catch (Exception ex) {
            log.error("Authentication extraction failed: {}", ex.getMessage(), ex);
            return Mono.error(ex);
        }
    }
}
