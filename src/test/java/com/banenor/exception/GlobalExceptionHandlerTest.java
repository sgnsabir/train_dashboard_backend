package com.banenor.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = GlobalExceptionHandler.class)
@TestPropertySource(locations = "classpath:test.properties")
public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockServerWebExchange exchange;

    @BeforeEach
    public void setUp() {
        // Initialize the GlobalExceptionHandler from the Spring context
        handler = new GlobalExceptionHandler();
        exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());
    }

    @Test
    public void testHandleAllExceptions() {
        Exception ex = new Exception("Test exception");
        Mono<ApiError> result = handler.handleAllExceptions(ex, exchange);
        StepVerifier.create(result)
                .assertNext(error -> {
                    assertThat(error.getErrorCode()).isEqualTo("ERR-500");
                    assertThat(error.getMessage()).isEqualTo("Internal server error");
                    assertThat(error.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
                    assertThat(error.getCorrelationId()).isNotNull();
                    // Verify the exchange response status was set correctly.
                    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
                })
                .verifyComplete();
    }

    @Test
    public void testHandleValidationExceptions() {
        Object target = new Object();
        WebExchangeBindException ex = new WebExchangeBindException(
                null,
                new org.springframework.validation.BeanPropertyBindingResult(target, "dummyObject")
        );
        ex.getBindingResult().addError(new org.springframework.validation.FieldError("dummyObject", "dummyField", "must not be blank"));
        Mono<ApiError> result = handler.handleValidationExceptions(ex, exchange);
        StepVerifier.create(result)
                .assertNext(error -> {
                    assertThat(error.getErrorCode()).isEqualTo("ERR-400");
                    assertThat(error.getMessage()).contains("must not be blank");
                    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                })
                .verifyComplete();
    }

    @Test
    public void testHandleHttpMessageNotReadable() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Malformed JSON");
        Mono<ApiError> result = handler.handleHttpMessageNotReadable(ex, exchange);
        StepVerifier.create(result)
                .assertNext(error -> {
                    assertThat(error.getErrorCode()).isEqualTo("ERR-400");
                    assertThat(error.getMessage()).isEqualTo("Malformed JSON request");
                    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                })
                .verifyComplete();
    }

    @Test
    public void testHandleMethodArgumentTypeMismatch() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("value", String.class, "param", null, new Exception("Type error"));
        Mono<ApiError> result = handler.handleMethodArgumentTypeMismatch(ex, exchange);
        StepVerifier.create(result)
                .assertNext(error -> {
                    assertThat(error.getErrorCode()).isEqualTo("ERR-400");
                    assertThat(error.getMessage()).contains("Invalid parameter: param");
                    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                })
                .verifyComplete();
    }

    @Test
    public void testHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource not found");
        Mono<ApiError> result = handler.handleResourceNotFound(ex, exchange);
        StepVerifier.create(result)
                .assertNext(error -> {
                    assertThat(error.getErrorCode()).isEqualTo("ERR-404");
                    assertThat(error.getMessage()).isEqualTo("Resource not found");
                    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
                })
                .verifyComplete();
    }

    @Test
    public void testHandleInvalidCredentials() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Bad credentials");
        Mono<ApiError> result = handler.handleInvalidCredentials(ex, exchange);
        StepVerifier.create(result)
                .assertNext(error -> {
                    assertThat(error.getErrorCode()).isEqualTo("ERR-401");
                    assertThat(error.getMessage()).isEqualTo("Invalid username or password");
                    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
                })
                .verifyComplete();
    }

    @Test
    public void testHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Illegal argument provided");
        Mono<ApiError> result = handler.handleIllegalArgumentException(ex, exchange);
        StepVerifier.create(result)
                .assertNext(error -> {
                    assertThat(error.getErrorCode()).isEqualTo("ERR-400");
                    assertThat(error.getMessage()).isEqualTo("Illegal argument provided");
                    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                })
                .verifyComplete();
    }

    @Test
    public void testHandleNullPointerException() {
        NullPointerException ex = new NullPointerException("Null pointer");
        Mono<ApiError> result = handler.handleNullPointerException(ex, exchange);
        StepVerifier.create(result)
                .assertNext(error -> {
                    assertThat(error.getErrorCode()).isEqualTo("ERR-500");
                    assertThat(error.getMessage()).isEqualTo("A required value was null");
                    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
                })
                .verifyComplete();
    }

    @Test
    public void testHandleIllegalStateException() {
        IllegalStateException ex = new IllegalStateException("Illegal state occurred");
        Mono<ApiError> result = handler.handleIllegalStateException(ex, exchange);
        StepVerifier.create(result)
                .assertNext(error -> {
                    assertThat(error.getErrorCode()).isEqualTo("ERR-500");
                    assertThat(error.getMessage()).isEqualTo("Invalid application state");
                    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
                })
                .verifyComplete();
    }
}
