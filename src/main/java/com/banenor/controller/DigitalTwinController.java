package com.banenor.controller;

import com.banenor.dto.DigitalTwinDTO;
import com.banenor.service.DigitalTwinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * REST controller for Digital Twin endpoints.
 * Provides endpoints for retrieving the digital twin state and streaming real-time updates via Server-Sent Events (SSE).
 */
@RestController
@RequestMapping("/api/v1/digital-twin")
@Tag(name = "Digital Twin", description = "Endpoints for digital twin integration and real-time updates")
public class DigitalTwinController {

    private final DigitalTwinService digitalTwinService;

    public DigitalTwinController(DigitalTwinService digitalTwinService) {
        this.digitalTwinService = digitalTwinService;
    }

    @Operation(summary = "Get Digital Twin State", description = "Retrieve the current state of the digital twin for a given asset")
    @GetMapping("/{assetId}")
    public Mono<ResponseEntity<DigitalTwinDTO>> getDigitalTwinState(@PathVariable Integer assetId) {
        return digitalTwinService.getTwinState(assetId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Stream Digital Twin Updates", description = "Stream real-time digital twin updates for a given asset via Server-Sent Events (SSE)")
    @GetMapping(value = "/stream/{assetId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DigitalTwinDTO> streamDigitalTwinUpdates(@PathVariable Integer assetId) {
        // Poll the digital twin state every 5 seconds and stream updates.
        return Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick -> digitalTwinService.getTwinState(assetId))
                .distinctUntilChanged(); // Only emit when the twin state changes.
    }
}
