package com.banenor.controller;

import com.banenor.dto.PredictiveMaintenanceDTO;
import com.banenor.service.PredictiveMaintenanceService;
import com.banenor.service.RealtimeAlertService;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/realtime")
@Tag(name = "Realtime", description = "Endpoints for realtime metrics, alert triggering, and SSE")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class RealtimeController {

    private static final String DEFAULT_ALERT_EMAIL = "alerts@example.com";

    private final RealtimeAlertService realtimeAlertService;
    private final PredictiveMaintenanceService predictiveMaintenanceService;
    private final Optional<SimpMessagingTemplate> messagingTemplate;
    private final MeterRegistry meterRegistry;

    @Operation(
            summary = "Get Latest Realtime Metrics",
            description = "Fetch the latest predictive maintenance metrics and risk assessment for a given analysis ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metrics retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid analysis ID"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/metrics/{analysisId}")
    public Mono<ResponseEntity<PredictiveMaintenanceDTO>> getLatestRealtimeMetrics(
            @Parameter(description = "Analysis ID", required = true)
            @PathVariable @Min(1) Integer analysisId) {

        meterRegistry.counter("realtime.metrics.requests", "analysisId", analysisId.toString()).increment();

        return predictiveMaintenanceService.getMaintenanceAnalysis(analysisId, DEFAULT_ALERT_EMAIL)
                .map(ResponseEntity::ok)
                .doOnSuccess(resp -> meterRegistry
                        .counter("realtime.metrics.success", "analysisId", analysisId.toString())
                        .increment())
                .doOnError(ex -> {
                    meterRegistry.counter("realtime.metrics.errors",
                            "analysisId", analysisId.toString(),
                            "error", ex.getClass().getSimpleName()).increment();
                    log.error("Error fetching realtime metrics for {}: {}", analysisId, ex.getMessage(), ex);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(
            summary = "Trigger Realtime Alert",
            description = "Evaluate and notify if any sensor exceeds its realtime threshold for the given analysis ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert processed"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Processing error")
    })
    @PostMapping("/alert/{analysisId}")
    public Mono<ResponseEntity<Void>> triggerRealtimeAlert(
            @Parameter(description = "Analysis ID", required = true)
            @PathVariable @Min(1) Integer analysisId,
            @Parameter(description = "Email for alert notifications")
            @RequestParam(defaultValue = DEFAULT_ALERT_EMAIL) @Email String alertEmail) {

        meterRegistry.counter("realtime.alerts.requests",
                "analysisId", analysisId.toString(),
                "email", alertEmail).increment();

        return realtimeAlertService
                .monitorAndAlert(analysisId, alertEmail)
                .doOnSuccess(ignored -> {
                    meterRegistry.counter("realtime.alerts.success", "analysisId", analysisId.toString()).increment();
                    messagingTemplate.ifPresent(t ->
                            t.convertAndSend("/topic/alerts",
                                    "Realtime alert processed for analysisId: " + analysisId)
                    );
                })
                // <- here we explicitly return a Void‐typed ResponseEntity
                .then(Mono.just(ResponseEntity.<Void>ok(null)))
                .onErrorResume(ex -> {
                    meterRegistry.counter("realtime.alerts.errors",
                            "analysisId", analysisId.toString(),
                            "error", ex.getClass().getSimpleName()).increment();
                    log.error("Error triggering realtime alert for {}: {}", analysisId, ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(
            summary = "Stream Realtime Metrics (SSE)",
            description = "Continuously stream the latest metrics every 5 seconds via Server‑Sent Events"
    )
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PredictiveMaintenanceDTO>> streamRealtimeMetrics(
            @RequestParam("trainNo") @Min(1) Integer trainNo) {

        log.info("Starting SSE stream for trainNo={}", trainNo);

        return Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick -> predictiveMaintenanceService.getMaintenanceAnalysis(trainNo, DEFAULT_ALERT_EMAIL)
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume(ex -> {
                            log.error("Error in SSE stream for {}: {}", trainNo, ex.getMessage());
                            return Mono.empty();
                        }))
                .map(data -> ServerSentEvent.<PredictiveMaintenanceDTO>builder()
                        .id(Long.toString(System.currentTimeMillis()))
                        .event("sensor-metrics")
                        .data(data)
                        .build())
                .doOnCancel(() -> log.info("SSE stream cancelled for trainNo={}", trainNo));
    }
}
