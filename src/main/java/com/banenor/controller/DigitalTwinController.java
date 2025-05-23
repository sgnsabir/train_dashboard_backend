package com.banenor.controller;

import com.banenor.dto.CameraPose;
import com.banenor.service.DigitalTwinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/digital-twin")
@Tag(name = "Digital Twin", description = "Real-time 3D pose streaming")
public class DigitalTwinController {

    private final DigitalTwinService service;

    public DigitalTwinController(DigitalTwinService service) {
        this.service = service;
    }

    @Operation(summary = "Get current camera pose")
    @GetMapping("/{assetId}")
    public Mono<ResponseEntity<CameraPose>> getPose(@PathVariable Integer assetId) {
        return service.getTwinState(assetId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Stream camera pose via SSE")
    @GetMapping(value = "/stream/{assetId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<CameraPose> streamPose(@PathVariable Integer assetId) {
        return service.streamDigitalTwinUpdates(assetId)
                .doOnCancel(() -> log.info("[DigitalTwinController] SSE stream cancelled for {}", assetId));
    }
}
