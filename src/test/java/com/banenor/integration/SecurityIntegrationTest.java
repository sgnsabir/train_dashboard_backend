package com.banenor.integration;

import com.banenor.PredictiveMaintenanceBackendApplication;
import com.banenor.dto.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@SpringBootTest(
        classes = {PredictiveMaintenanceBackendApplication.class, SecurityIntegrationTest.TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@TestPropertySource("classpath:test.properties")
class SecurityIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    // Test configuration can be expanded as needed to add additional beans required for context loading.
    @Configuration
    static class TestConfig {
        // If additional test-specific bean definitions are required, define them here.
        // For example, if your security configuration needs custom user details service,
        // you might provide a test version. Otherwise, rely on the main application's beans.
        //
        // Uncomment and modify the following as needed:
        //
        // @Bean
        // public ReactiveUserDetailsService reactiveUserDetailsService() {
        //     // Return a test implementation or delegate to a mock.
        //     return username -> Mono.just(
        //             org.springframework.security.core.userdetails.User
        //                     .withUsername(username)
        //                     .password("{noop}password")
        //                     .authorities("ROLE_ADMIN")
        //                     .build());
        // }
    }

    @Test
    @DisplayName("Login and access secured endpoint with valid token")
    void testLoginAndAccessSecuredEndpoint() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password");

        byte[] loginResponseBody = webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(loginRequest))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult().getResponseBody();

        assertNotNull(loginResponseBody, "Login response body should not be null");

        JsonNode loginResponseJson = objectMapper.readTree(loginResponseBody);
        String token = loginResponseJson.get("token").asText();
        assertNotNull(token, "JWT token should not be null");

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .get()
                .uri("/api/v1/admin/dashboard")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Register, login and access protected endpoint (non-admin user)")
    void testUserRegistrationAndLoginAndAccessProtectedEndpoint() throws Exception {
        String registerResponse = webTestClient.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"testuser\",\"email\":\"testuser@example.com\",\"password\":\"Test@123\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();
        assertNotNull(registerResponse);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Test@123");

        byte[] loginResponseBody = webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(loginRequest))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult().getResponseBody();

        JsonNode loginResponseJson = objectMapper.readTree(loginResponseBody);
        String token = loginResponseJson.get("token").asText();
        assertNotNull(token, "JWT token should not be null");

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockUser().roles("USER"))
                .get()
                .uri("/api/v1/admin/dashboard")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Login with invalid credentials should fail")
    void testLoginWithInvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("wrongPassword");

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(loginRequest))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Accessing secured endpoint without token should be unauthorized")
    void testAccessProtectedEndpointWithoutToken() {
        webTestClient.get()
                .uri("/api/v1/admin/dashboard")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Password reset flow works correctly")
    void testResetPasswordIntegration() throws Exception {
        String registerResponse = webTestClient.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"resetUser\",\"email\":\"resetuser@example.com\",\"password\":\"Initial@123\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();
        assertNotNull(registerResponse);

        String resetResponse = webTestClient.post()
                .uri("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"resetuser@example.com\",\"newPassword\":\"NewPass@123\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();
        assertNotNull(resetResponse);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("resetUser");
        loginRequest.setPassword("NewPass@123");

        byte[] loginResponseBody = webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(loginRequest))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult().getResponseBody();

        JsonNode loginResponseJson = objectMapper.readTree(loginResponseBody);
        String token = loginResponseJson.get("token").asText();
        assertNotNull(token, "JWT token should not be null after password reset login");
    }

    @Test
    @DisplayName("Logout invalidates the token")
    void testLogoutIntegration() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password");

        byte[] loginResponseBody = webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(loginRequest))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult().getResponseBody();

        JsonNode loginResponseJson = objectMapper.readTree(loginResponseBody);
        String token = loginResponseJson.get("token").asText();
        assertNotNull(token, "JWT token should not be null");

        String logoutResponse = webTestClient.post()
                .uri("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertNotNull(logoutResponse);

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockUser().roles("ADMIN"))
                .get()
                .uri("/api/v1/admin/dashboard")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
