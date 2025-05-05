package com.banenor.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Order(0)
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String newCorrelation() {
        return UUID.randomUUID().toString();
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ApiError> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String cid = newCorrelation();
        BindingResult br = ex.getBindingResult();
        List<String> details = br.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        logger.warn("Validation failed [cid={}]: {}", cid, details);
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        return Mono.just(new ApiError(
                "ERR-400",
                "Validation failed",
                details,
                LocalDateTime.now(),
                cid
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ApiError> handleConstraintViolation(ConstraintViolationException ex, ServerWebExchange exchange) {
        String cid = newCorrelation();
        List<String> details = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.toList());
        logger.warn("Constraint violations [cid={}]: {}", cid, details);
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        return Mono.just(new ApiError(
                "ERR-400",
                "Validation failed",
                details,
                LocalDateTime.now(),
                cid
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Mono<ApiError> handleMalformedJson(HttpMessageNotReadableException ex, ServerWebExchange exchange) {
        String cid = newCorrelation();
        logger.warn("Malformed JSON [cid={}]: {}", cid, ex.getMessage());
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        return Mono.just(new ApiError(
                "ERR-400",
                "Malformed JSON request",
                List.of(ex.getMessage()),
                LocalDateTime.now(),
                cid
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Mono<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, ServerWebExchange exchange) {
        String cid = newCorrelation();
        String msg = ex.getName() + " should be of type " + ex.getRequiredType().getSimpleName();
        logger.warn("Type mismatch [cid={}]: {}", cid, msg);
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        return Mono.just(new ApiError(
                "ERR-400",
                "Type mismatch",
                List.of(msg),
                LocalDateTime.now(),
                cid
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ApiError> handleNotFound(ResourceNotFoundException ex, ServerWebExchange exchange) {
        String cid = newCorrelation();
        logger.warn("Not found [cid={}]: {}", cid, ex.getMessage());
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
        return Mono.just(new ApiError(
                "ERR-404",
                ex.getMessage(),
                List.of(),
                LocalDateTime.now(),
                cid
        ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public Mono<ApiError> handleAuthFail(InvalidCredentialsException ex, ServerWebExchange exchange) {
        String cid = newCorrelation();
        logger.warn("Auth failed [cid={}]: {}", cid, ex.getMessage());
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return Mono.just(new ApiError(
                "ERR-401",
                "Invalid username or password",
                List.of(),
                LocalDateTime.now(),
                cid
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ApiError> handleBadArg(IllegalArgumentException ex, ServerWebExchange exchange) {
        String cid = newCorrelation();
        logger.error("Bad argument [cid={}]: {}", cid, ex.getMessage());
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        return Mono.just(new ApiError(
                "ERR-400",
                ex.getMessage(),
                List.of(),
                LocalDateTime.now(),
                cid
        ));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ApiError> handleAll(Exception ex, ServerWebExchange exchange) {
        String cid = newCorrelation();
        logger.error("Internal error [cid={}]: {}", cid, ex.getMessage(), ex);
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return Mono.just(new ApiError(
                "ERR-500",
                "Internal server error",
                List.of("An unexpected error occurred"),
                LocalDateTime.now(),
                cid
        ));
    }
}
