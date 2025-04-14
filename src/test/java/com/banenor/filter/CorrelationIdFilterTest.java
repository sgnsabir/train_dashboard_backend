package com.banenor.filter;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(locations = "classpath:test.properties")
class CorrelationIdFilterTest {

    @Test
    void testCorrelationIdFilter_SetsMDC_ProvidedHeader() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        String testId = "test-correlation-id";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").header("X-Correlation-Id", testId).build()
        );

        WebFilterChain chain = ex -> {
            String correlationId = MDC.get("correlationId");
            assertEquals(testId, correlationId, "MDC should contain the provided correlation id");
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertNull(MDC.get("correlationId"), "MDC correlationId should be cleared after filter chain completes");
    }

    @Test
    void testCorrelationIdFilter_GeneratesNewIdWhenMissing() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build()
        );

        WebFilterChain chain = ex -> {
            String correlationId = MDC.get("correlationId");
            assertNotNull(correlationId, "A generated correlationId should be present when header is missing");
            assertTrue(correlationId.matches("^[0-9a-fA-F\\-]{36}$"), "Generated correlationId should be a valid UUID");
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertNull(MDC.get("correlationId"), "MDC correlationId should be cleared after chain processing");
    }

    @Test
    void testCorrelationIdFilter_GeneratesNewIdWhenEmpty() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").header("X-Correlation-Id", "").build()
        );

        WebFilterChain chain = ex -> {
            String correlationId = MDC.get("correlationId");
            assertNotNull(correlationId, "A generated correlationId should be present when header is empty");
            assertTrue(correlationId.matches("^[0-9a-fA-F\\-]{36}$"), "Generated correlationId should be a valid UUID");
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertNull(MDC.get("correlationId"), "MDC correlationId should be cleared after chain processing");
    }

    @Test
    void testCorrelationIdFilter_ClearsMDCOnChainException() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").header("X-Correlation-Id", "exception-test-id").build()
        );

        WebFilterChain chain = ex -> Mono.error(new RuntimeException("Test Exception"));

        StepVerifier.create(filter.filter(exchange, chain))
                .expectError(RuntimeException.class)
                .verify();

        assertNull(MDC.get("correlationId"), "MDC correlationId should be cleared even when chain throws an exception");
    }
}
