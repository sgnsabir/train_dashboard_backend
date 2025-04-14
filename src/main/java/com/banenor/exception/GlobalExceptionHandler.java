package com.banenor.exception;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Order(0) // High precedence for exception handling
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public Mono<ApiError> handleAllExceptions(Exception ex, ServerWebExchange exchange) {
        String correlationId = UUID.randomUUID().toString();
        logger.error("Unhandled exception [correlationId={}]: {}", correlationId, ex.getMessage(), ex);
        ApiError error = new ApiError("ERR-500", "Internal server error", LocalDateTime.now(), correlationId);
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return Mono.just(error);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ApiError> handleValidationExceptions(WebExchangeBindException ex, ServerWebExchange exchange) {
        String correlationId = UUID.randomUUID().toString();
        Map<String, String> errors = new HashMap<>();
        ex.getFieldErrors().forEach(fieldError -> errors.put(fieldError.getField(), fieldError.getDefaultMessage()));
        logger.warn("Validation error [correlationId={}]: {}", correlationId, errors);
        ApiError error = new ApiError("ERR-400", "Validation Failed: " + errors.toString(), LocalDateTime.now(), correlationId);
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        return Mono.just(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Mono<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, ServerWebExchange exchange) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Malformed JSON request [correlationId={}]: {}", correlationId, ex.getMessage());
        ApiError error = new ApiError("ERR-400", "Malformed JSON request", LocalDateTime.now(), correlationId);
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        return Mono.just(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Mono<ApiError> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, ServerWebExchange exchange) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Type mismatch [correlationId={}]: {}", correlationId, ex.getMessage());
        ApiError error = new ApiError("ERR-400", "Invalid parameter: " + ex.getName(), LocalDateTime.now(), correlationId);
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        return Mono.just(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ApiError> handleResourceNotFound(ResourceNotFoundException ex, ServerWebExchange exchange) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Resource not found [correlationId={}]: {}", correlationId, ex.getMessage());
        ApiError error = new ApiError("ERR-404", ex.getMessage(), LocalDateTime.now(), correlationId);
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
        return Mono.just(error);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public Mono<ApiError> handleInvalidCredentials(InvalidCredentialsException ex, ServerWebExchange exchange) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Invalid credentials [correlationId={}]: {}", correlationId, ex.getMessage());
        ApiError error = new ApiError("ERR-401", "Invalid username or password", LocalDateTime.now(), correlationId);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return Mono.just(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ApiError> handleIllegalArgumentException(IllegalArgumentException ex, ServerWebExchange exchange) {
        String correlationId = UUID.randomUUID().toString();
        logger.error("IllegalArgumentException [correlationId={}]: {}", correlationId, ex.getMessage());
        ApiError error = new ApiError("ERR-400", ex.getMessage(), LocalDateTime.now(), correlationId);
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        return Mono.just(error);
    }

    @ExceptionHandler(NullPointerException.class)
    public Mono<ApiError> handleNullPointerException(NullPointerException ex, ServerWebExchange exchange) {
        String correlationId = UUID.randomUUID().toString();
        logger.error("NullPointerException [correlationId={}]: {}", correlationId, ex.getMessage());
        ApiError error = new ApiError("ERR-500", "A required value was null", LocalDateTime.now(), correlationId);
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return Mono.just(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public Mono<ApiError> handleIllegalStateException(IllegalStateException ex, ServerWebExchange exchange) {
        String correlationId = UUID.randomUUID().toString();
        logger.error("IllegalStateException [correlationId={}]: {}", correlationId, ex.getMessage());
        ApiError error = new ApiError("ERR-500", "Invalid application state", LocalDateTime.now(), correlationId);
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return Mono.just(error);
    }
}
