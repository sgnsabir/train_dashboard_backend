package com.banenor.controller;

import com.banenor.dto.PredictiveMaintenanceDTO;
import com.banenor.service.PredictiveMaintenanceService;
import com.banenor.service.RealtimeAlertService;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Realtime", description = "Realtime aliases for predictive maintenance and alerting")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class RealtimeController {

    private static final String DEFAULT_ALERT_EMAIL = "alerts@example.com";

    private final PredictiveMaintenanceService predictiveMaintenanceService;
    private final RealtimeAlertService realtimeAlertService;
    private final Optional<SimpMessagingTemplate> messagingTemplate;
    private final MeterRegistry meterRegistry;

    /**
     * Alias for /api/v1/predictive/{analysisId}.
     * Retrieves predictive maintenance insights.
     */
    @Operation(
            summary = "Get Latest Realtime Metrics",
            description = "Alias to fetch predictive maintenance insights for a given analysis ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Predictive maintenance data retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid analysis ID"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/metrics/{analysisId}")
    public Mono<ResponseEntity<PredictiveMaintenanceDTO>> getLatestRealtimeMetrics(
            @Parameter(description = "Analysis ID", required = true)
            @PathVariable @Min(1) Integer analysisId
    ) {
        meterRegistry.counter("realtime.metrics.requests", "analysisId", analysisId.toString()).increment();

        return predictiveMaintenanceService
                .getMaintenanceAnalysis(analysisId, DEFAULT_ALERT_EMAIL)
                .map(dto -> {
                    meterRegistry.counter("realtime.metrics.success", "analysisId", analysisId.toString()).increment();
                    return ResponseEntity.ok(dto);
                })
                .doOnError(ex -> {
                    meterRegistry.counter("realtime.metrics.errors",
                            "analysisId", analysisId.toString(),
                            "error", ex.getClass().getSimpleName()).increment();
                    log.error("Error fetching predictive maintenance for analysisId {}: {}", analysisId, ex.getMessage(), ex);
                })
                .onErrorResume(ex -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Evaluates realtime thresholds and sends alerts if needed.
     */
    @Operation(
            summary = "Trigger Realtime Alert",
            description = "Evaluate sensors against realtime thresholds and notify if any exceed limits"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Realtime alert processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Processing error")
    })
    @PostMapping("/alert/{analysisId}")
    public Mono<ResponseEntity<Void>> triggerRealtimeAlert(
            @Parameter(description = "Analysis ID", required = true)
            @PathVariable @Min(1) Integer analysisId,
            @Parameter(description = "Email for alert notifications")
            @RequestParam(defaultValue = DEFAULT_ALERT_EMAIL) @Email String alertEmail
    ) {
        meterRegistry.counter("realtime.alerts.requests",
                "analysisId", analysisId.toString(),
                "email", alertEmail).increment();

        return realtimeAlertService.monitorAndAlert(analysisId, alertEmail)
                .doOnSuccess(v -> {
                    meterRegistry.counter("realtime.alerts.success", "analysisId", analysisId.toString()).increment();
                    messagingTemplate.ifPresent(t ->
                            t.convertAndSend("/topic/alerts", "Realtime alert processed for analysisId: " + analysisId)
                    );
                })
                .thenReturn(ResponseEntity.ok().<Void>build())
                .onErrorResume(ex -> {
                    meterRegistry.counter("realtime.alerts.errors",
                            "analysisId", analysisId.toString(),
                            "error", ex.getClass().getSimpleName()).increment();
                    log.error("Error triggering realtime alert for analysisId {}: {}", analysisId, ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Streams predictive maintenance updates every 5 seconds via SSE.
     */
    @Operation(
            summary = "Stream Realtime Metrics (SSE)",
            description = "Continuously stream predictive maintenance updates every 5 seconds"
    )
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PredictiveMaintenanceDTO>> streamRealtimeMetrics(
            @RequestParam("analysisId") @Min(1) Integer analysisId
    ) {
        log.info("Starting SSE stream for analysisId={}", analysisId);

        return Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick ->
                        predictiveMaintenanceService.getMaintenanceAnalysis(analysisId, DEFAULT_ALERT_EMAIL)
                                .subscribeOn(Schedulers.boundedElastic())
                                .onErrorResume(ex -> {
                                    log.error("Error in SSE stream for analysisId {}: {}", analysisId, ex.getMessage());
                                    return Mono.empty();
                                })
                )
                .map(data -> ServerSentEvent.<PredictiveMaintenanceDTO>builder()
                        .id(Long.toString(System.currentTimeMillis()))
                        .event("predictive-maintenance-update")
                        .data(data)
                        .build()
                )
                .doOnCancel(() -> log.info("SSE stream cancelled for analysisId={}", analysisId));
    }
}
