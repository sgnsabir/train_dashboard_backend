package com.banenor.controller;

import com.banenor.service.SystemHealthService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Slf4j
public class SystemHealthController {

    private final SystemHealthService healthService;

    /**
     * Health check endpoint returning system component statuses.
     * • 200 OK if all dependencies are operational
     * • 503 Service Unavailable otherwise
     */
    @Timed(value = "system.health.endpoint", description = "Execution time of the system health check")
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<HealthResponse>> health() {
        log.debug("Received system health‑check request");
        return healthService.getSystemStatus()
                .map(status -> {
                    boolean up = "Operational".equalsIgnoreCase(status);
                    HttpStatus code = up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
                    log.info("Health‑check result: {} → HTTP {}", status, code.value());
                    return ResponseEntity
                            .status(code)
                            .body(new HealthResponse(status, Instant.now()));
                })
                .onErrorResume(ex -> {
                    log.error("Health‑check failed", ex);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body(new HealthResponse("DOWN", Instant.now())));
                });
    }

    /**
     * Simple DTO for the health-check response.
     */
    public record HealthResponse(String status, Instant timestamp) {}
}
