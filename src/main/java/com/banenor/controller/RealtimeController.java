package com.banenor.controller;

import com.banenor.dto.PredictiveMaintenanceResponse;
import com.banenor.dto.RealtimeAlertRequest;
import com.banenor.service.PredictiveMaintenanceService;
import com.banenor.service.RealtimeAlertService;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Validated
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

    @Operation(
            summary = "Get Latest Realtime Metrics",
            description = "Alias to fetch predictive maintenance insights for a given analysis ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Predictive maintenance data retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/metrics/{analysisId}")
    public Mono<ResponseEntity<PredictiveMaintenanceResponse>> getLatestRealtimeMetrics(
            @PathVariable @Min(1) Integer analysisId
    ) {
        meterRegistry.counter("realtime.metrics.requests", "analysisId", analysisId.toString()).increment();

        return predictiveMaintenanceService.getMaintenanceAnalysis(analysisId, DEFAULT_ALERT_EMAIL)
                .map(dto -> {
                    meterRegistry.counter("realtime.metrics.success", "analysisId", analysisId.toString()).increment();
                    log.info("Fetched realtime metrics for analysisId={}", analysisId);
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

    @Operation(
            summary = "Trigger Realtime Alert",
            description = "Evaluate sensors against realtime thresholds and notify if any exceed limits"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Realtime alert processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
            @ApiResponse(responseCode = "500", description = "Processing error")
    })
    @PostMapping("/alert")
    public Mono<ResponseEntity<Void>> triggerRealtimeAlert(
            @Valid @RequestBody RealtimeAlertRequest req
    ) {
        Integer analysisId = req.getAnalysisId();
        String alertEmail = req.getAlertEmail();

        meterRegistry.counter("realtime.alerts.requests",
                "analysisId", analysisId.toString(),
                "email", alertEmail).increment();

        log.info("Triggering realtime alert for analysisId={} with email={}", analysisId, alertEmail);

        return realtimeAlertService.monitorAndAlert(analysisId, alertEmail)
                .doOnSuccess(v -> {
                    meterRegistry.counter("realtime.alerts.success", "analysisId", analysisId.toString()).increment();
                    log.info("Realtime alert processed successfully for analysisId={}", analysisId);
                    messagingTemplate.ifPresent(t ->
                            t.convertAndSend("/topic/alerts", "Realtime alert processed for analysisId: " + analysisId)
                    );
                })
                .thenReturn(ResponseEntity.ok().<Void>build())
                .doOnError(ex -> {
                    meterRegistry.counter("realtime.alerts.errors",
                            "analysisId", analysisId.toString(),
                            "error", ex.getClass().getSimpleName()).increment();
                    log.error("Error triggering realtime alert for analysisId {}: {}", analysisId, ex.getMessage(), ex);
                })
                .onErrorResume(ex -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(
            summary = "Stream Realtime Metrics (SSE)",
            description = "Continuously stream predictive maintenance updates every 5 seconds"
    )
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PredictiveMaintenanceResponse>> streamRealtimeMetrics(
            @RequestParam("analysisId") @Min(1) Integer analysisId
    ) {
        log.info("Starting SSE stream for analysisId={}", analysisId);

        return Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick ->
                        predictiveMaintenanceService.getMaintenanceAnalysis(analysisId, DEFAULT_ALERT_EMAIL)
                                .subscribeOn(Schedulers.boundedElastic())
                                .onErrorResume(ex -> {
                                    log.error("Error in SSE stream for analysisId {}: {}", analysisId, ex.getMessage(), ex);
                                    return Mono.empty();
                                })
                )
                .map(data -> ServerSentEvent.<PredictiveMaintenanceResponse>builder()
                        .id(Long.toString(System.currentTimeMillis()))
                        .event("predictive-maintenance-update")
                        .data(data)
                        .build()
                )
                .doOnCancel(() -> log.info("SSE stream cancelled for analysisId={}", analysisId));
    }
}
