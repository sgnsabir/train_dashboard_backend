package com.banenor.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
@Order(-2)
public class ApiErrorHandler {

    private String newCorrelationId() {
        return UUID.randomUUID().toString();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ApiError> handleResponseStatus(ResponseStatusException ex, ServerWebExchange exchange) {
        String cid = newCorrelationId();

        // ResponseStatusException#getStatusCode() returns HttpStatusCode; coerce to HttpStatus
        HttpStatusCode raw = ex.getStatusCode();
        HttpStatus status = (raw instanceof HttpStatus)
                ? (HttpStatus) raw
                : HttpStatus.valueOf(raw.value());

        log.warn("ResponseStatusException [cid={}]: {} {}", cid, status, ex.getMessage());
        exchange.getResponse().setStatusCode(status);

        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return Mono.just(new ApiError(
                "ERR-" + status.value(),
                message,
                List.of(ex.getMessage()),
                LocalDateTime.now(),
                cid
        ));
    }

    @ExceptionHandler(Throwable.class)
    public Mono<ApiError> handleAll(Throwable ex, ServerWebExchange exchange) {
        String cid = newCorrelationId();
        log.error("Unhandled error [cid={}]: {}", cid, ex.getMessage(), ex);
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

        return Mono.just(new ApiError(
                "ERR-500",
                "Internal server error",
                List.of(ex.getMessage()),
                LocalDateTime.now(),
                cid
        ));
    }
}
