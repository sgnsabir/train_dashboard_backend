// src/main/java/com/banenor/controller/SystemHealthController.java
package com.banenor.controller;

import com.banenor.service.SystemHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemHealthController {

    private final SystemHealthService systemHealthService;

    /**
     * GET /api/v1/system/health
     * Retrieves the current system health status.
     * The SystemHealthService aggregates the health of R2DBC, Kafka, and Redis.
     * Expected backend response: { "status": "Operational" } (or similar status).
     *
     * @return a Mono emitting a ResponseEntity containing a JSON map with the key "status".
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, String>>> getSystemHealth() {
        return systemHealthService.getSystemStatus()
                .map(status -> {
                    log.info("System health retrieved successfully: {}", status);
                    return ResponseEntity.ok(Collections.singletonMap("status", status));
                })
                .doOnError(e -> log.error("Error retrieving system health status", e))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Collections.singletonMap("status", "Internal server error: " + e.getMessage())))
                );
    }
}
