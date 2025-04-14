package com.banenor.filter;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;

@TestPropertySource(locations = "classpath:test.properties")
@DisplayName("RateLimitingFilter Integration Tests")
class RateLimitingFilterTest {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private WebTestClient webTestClient;
    private MeterRegistry meterRegistry;

    // Dummy controller to simulate an endpoint that always returns "OK"
    @RestController
    static class DummyController {
        @GetMapping("/")
        public Mono<String> ok() {
            return Mono.just("OK");
        }
    }

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        webTestClient = WebTestClient.bindToController(new DummyController())
                .webFilter(new RateLimitingFilter(meterRegistry))
                .build();
    }

    @Test
    @DisplayName("Allows requests below rate limit and rejects requests exceeding the limit")
    void testRateLimitingFilter_EnforcesLimit() {
        String testIp = "192.168.1.100";

        // Send MAX_REQUESTS_PER_MINUTE requests – expect HTTP 200 for each.
        for (int i = 1; i <= MAX_REQUESTS_PER_MINUTE; i++) {
            webTestClient.get()
                    .uri("/")
                    .header("X-Forwarded-For", testIp)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(String.class)
                    .value(body -> assertThat(body).isEqualTo("OK"));
        }

        // The 61st request should be rejected.
        webTestClient.get()
                .uri("/")
                .header("X-Forwarded-For", testIp)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectBody(String.class)
                .value(responseBody ->
                        assertThat(responseBody).contains("Too many requests - Rate limit exceeded"));
    }

    @Test
    @DisplayName("If no X-Forwarded-For header is present, fallback to remote address")
    void testRateLimitingFilter_FallbackToRemoteAddress() {
        // When no X-Forwarded-For header is provided, the filter uses the remote address.
        for (int i = 1; i <= MAX_REQUESTS_PER_MINUTE; i++) {
            webTestClient.get()
                    .uri("/")
                    .exchange()
                    .expectStatus().isOk();
        }
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
