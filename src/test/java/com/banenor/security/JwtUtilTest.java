package com.banenor.security;

import java.util.Date;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;

@SpringBootTest(classes = JwtUtil.class)
@TestPropertySource(locations = "classpath:test.properties")
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Ensure that the JwtTokenUtil is initialized with test properties.
     * If "jwt.secret" is not set by the test properties, then use a default test secret.
     */
    private void ensureTestSecretIsSet() {
        String secret = (String) ReflectionTestUtils.getField(jwtUtil, "jwtSecret");
        if (secret == null || secret.trim().isEmpty()) {
            // Use a default test secret (base64 encoded string for "This is a test Secret")
            ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "VGhpcyBpcyBhIHRlc3QgU2VjcmV0");
            // Also set the expiration from test.properties (if not already set)
            ReflectionTestUtils.setField(jwtUtil, "jwtExpirationSeconds", 3600L);
            jwtUtil.init();
        }
    }

    @Test
    void testGenerateTokenAndExtractUsername() {
        ensureTestSecretIsSet();
        String token = jwtUtil.generateToken("testuser", "[ROLE_USER]");
        assertNotNull(token, "Generated token should not be null");
        String username = jwtUtil.getUsernameFromToken(token);
        assertEquals("testuser", username, "Extracted username should match the one provided");
    }

    @Test
    void testTokenExpiration() {
        ensureTestSecretIsSet();
        String token = jwtUtil.generateToken("testuser", "[ROLE_USER]");
        Date expiration = jwtUtil.getExpirationDateFromToken(token);
        assertNotNull(expiration, "Expiration date should not be null");
        long diff = expiration.getTime() - System.currentTimeMillis();
        // Check that the token is valid for approximately one hour.
        assertTrue(diff <= 3600L * 1000 && diff > 3500L * 1000,
                "Token expiration should be about one hour");
    }

    @Test
    void testValidateToken_Success() {
        ensureTestSecretIsSet();
        String token = jwtUtil.generateToken("testuser", "[ROLE_USER]");
        boolean valid = jwtUtil.validateToken(token, "testuser");
        assertTrue(valid, "Token should be valid for the correct username");
    }

    @Test
    void testValidateToken_FailOnUsernameMismatch() {
        ensureTestSecretIsSet();
        String token = jwtUtil.generateToken("testuser", "[ROLE_USER]");
        Exception exception = assertThrows(RuntimeException.class, () ->
                        jwtUtil.validateToken(token, "wronguser"),
                "Token validation should fail if username does not match");
        String message = exception.getMessage().toLowerCase();
        assertTrue(message.contains("mismatch") || message.contains("invalid"),
                "Exception message should indicate a username mismatch or invalid token");
    }

    @Test
    void testGetClaimFromToken_CustomLambda() {
        ensureTestSecretIsSet();
        String token = jwtUtil.generateToken("testuser", "[ROLE_ADMIN]");
        Function<Claims, Object> rolesExtractor = claims -> claims.get("roles");
        Object roles = jwtUtil.getClaimFromToken(token, rolesExtractor);
        assertNotNull(roles, "Roles claim should not be null");
        assertEquals("[ROLE_ADMIN]", roles.toString(), "Roles claim should match the provided value");
    }

    @Test
    void testValidateToken_MalformedToken() {
        ensureTestSecretIsSet();
        String malformedToken = "this.is.not.a.jwt";
        Exception exception = assertThrows(MalformedJwtException.class, () ->
                        jwtUtil.validateToken(malformedToken, "testuser"),
                "A malformed token should throw a MalformedJwtException");
        String message = exception.getMessage().toLowerCase();
        // Accept messages that mention "invalid jwt token", "malformed", or "expected format"
        assertTrue(message.contains("invalid jwt token")
                        || message.contains("malformed")
                        || message.contains("expected format"),
                "Exception should indicate the token is malformed");
    }

    @Test
    void testExpiredToken() throws InterruptedException {
        ensureTestSecretIsSet();
        // Temporarily override the expiration time to 1 second
        Object expirationObj = ReflectionTestUtils.getField(jwtUtil, "jwtExpirationSeconds");
        long originalExpiration = expirationObj != null ? (long) expirationObj : 3600L;
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationSeconds", 1L);
        jwtUtil.init();
        String token = jwtUtil.generateToken("testuser", "[ROLE_USER]");
        // Wait for the token to expire
        Thread.sleep(1500);
        Exception exception = assertThrows(RuntimeException.class, () ->
                        jwtUtil.validateToken(token, "testuser"),
                "Expired token should throw an exception");
        String message = exception.getMessage().toLowerCase();
        assertTrue(message.contains("expired"),
                "Exception message should indicate that the token has expired");
        // Reset the expiration time for subsequent tests
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationSeconds", originalExpiration);
        jwtUtil.init();
    }
}
