// src/main/java/com/banenor/controller/SystemHealthController.java
package com.banenor.controller;

import com.banenor.service.SystemHealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemHealthController {

    private static final Logger log = LoggerFactory.getLogger(SystemHealthController.class);

    private final SystemHealthService systemHealthService;

    public SystemHealthController(SystemHealthService systemHealthService) {
        this.systemHealthService = systemHealthService;
    }

    /**
     * GET /api/system/health
     * Retrieves the current system health status.
     * The SystemHealthService aggregates the health of R2DBC, Kafka, and Redis.
     * Expected backend response: { "status": "Operational" } (or similar status).
     *
     * @return a Mono emitting a ResponseEntity containing a JSON map with the key "status".
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, String>>> getSystemHealth() {
        return systemHealthService.getSystemStatus()
                // Log the successful retrieval of system health.
                .map(status -> {
                    log.info("System health retrieved successfully: {}", status);
                    return ResponseEntity.ok(Collections.singletonMap("status", status));
                })
                // Log any error that occurs during retrieval.
                .doOnError(e -> log.error("Error retrieving system health status", e))
                // On error, return a 500 Internal Server Error with a descriptive error message.
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Collections.singletonMap("status", "Internal server error: " + e.getMessage())))
                );
    }
}
