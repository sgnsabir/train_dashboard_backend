package com.banenor.controller;

import com.banenor.dto.HistoricalDataResponse;
import com.banenor.dto.PredictiveMaintenanceResponse;
import com.banenor.dto.RealtimeAlertRequest;
import com.banenor.dto.SensorMetricsDTO;
import com.banenor.dto.SystemDashboardDTO;
import com.banenor.service.DashboardService;
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
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Endpoints for retrieving dashboard and realtime metrics")
@Slf4j
@Validated
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class DashboardController {

    private static final String DEFAULT_ALERT_EMAIL = "alerts@example.com";

    private final DashboardService dashboardService;
    private final PredictiveMaintenanceService predictiveMaintenanceService;
    private final RealtimeAlertService realtimeAlertService;
    private final Optional<SimpMessagingTemplate> messagingTemplate;
    private final MeterRegistry meterRegistry;

    // ─────────── Dashboard Endpoints ───────────

    @Operation(summary = "Get Latest Metrics",
            description = "Retrieve the latest sensor metrics for a given analysis ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Latest metrics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis ID not found")
    })
    @GetMapping("/latest/{analysisId}")
    public Mono<ResponseEntity<SensorMetricsDTO>> getLatestMetrics(
            @PathVariable @Min(1) Integer analysisId
    ) {
        log.debug("Request GET /latest/{}", analysisId);
        return dashboardService.getLatestMetrics(analysisId)
                .map(ResponseEntity::ok)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> log.error("Error retrieving latest metrics for {}: {}", analysisId, ex.getMessage()));
    }

    @Operation(summary = "Get Historical Data",
            description = "Retrieve historical sensor data for a given analysis ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historical data retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis ID not found")
    })
    @GetMapping("/historical/{analysisId}")
    public Mono<ResponseEntity<HistoricalDataResponse>> getHistoricalData(
            @PathVariable @Min(1) Integer analysisId
    ) {
        log.debug("Request GET /historical/{}", analysisId);
        return dashboardService.getHistoricalData(analysisId)
                .map(ResponseEntity::ok)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> log.error("Error retrieving historical data for {}: {}", analysisId, ex.getMessage()));
    }

    @Operation(summary = "Get System Dashboard",
            description = "Retrieve global fleet metrics, recent alerts, and system health")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "System dashboard retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/global")
    public Mono<ResponseEntity<SystemDashboardDTO>> getSystemDashboard() {
        log.debug("Request GET /global");
        return dashboardService.getSystemDashboard()
                .map(ResponseEntity::ok)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> log.error("Error retrieving system dashboard: {}", ex.getMessage()));
    }

    // ─────────── Realtime Endpoints ───────────

    @Operation(summary = "Get Latest Realtime Metrics",
            description = "Alias to fetch predictive maintenance insights for a given analysis ID")
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
        log.debug("Request GET /metrics/{}", analysisId);

        return predictiveMaintenanceService.getMaintenanceAnalysis(analysisId, DEFAULT_ALERT_EMAIL)
                .subscribeOn(Schedulers.boundedElastic())
                .map(dto -> {
                    meterRegistry.counter("realtime.metrics.success", "analysisId", analysisId.toString()).increment();
                    log.info("Fetched realtime metrics for analysisId={}", analysisId);
                    return ResponseEntity.ok(dto);
                })
                .onErrorResume(ex -> {
                    meterRegistry.counter("realtime.metrics.errors", "analysisId", analysisId.toString(),
                            "error", ex.getClass().getSimpleName()).increment();
                    log.error("Error fetching predictive maintenance for analysisId {}: {}", analysisId, ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(
            summary     = "Trigger Realtime Alert",
            description = "Evaluate sensors against realtime thresholds and notify if any exceed limits; " +
                    "supports optional custom subject/message"
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
        String subject    = Optional.ofNullable(req.getSubject())
                .orElse("Realtime Alert for analysis " + analysisId);
        String message    = Optional.ofNullable(req.getMessage())
                .orElse("Threshold exceeded on analysis " + analysisId +
                        ". Please investigate.");

        meterRegistry.counter("realtime.alerts.requests",
                        "analysisId", analysisId.toString(),
                        "email", alertEmail)
                .increment();

        log.debug("POST /alert | analysisId={} email={} subject={}",
                analysisId, alertEmail, subject);

        return realtimeAlertService
                .monitorAndAlert(analysisId, alertEmail, subject, message)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> {
                    meterRegistry.counter("realtime.alerts.success",
                                    "analysisId", analysisId.toString())
                            .increment();
                    log.info("Realtime alert sent for analysisId={}", analysisId);
                    messagingTemplate.ifPresent(t ->
                            t.convertAndSend("/topic/alerts",
                                    Map.of(
                                            "analysisId", analysisId,
                                            "subject", subject,
                                            "message", message
                                    )
                            )
                    );
                })
                .thenReturn(ResponseEntity.ok().<Void>build())
                .onErrorResume(ex -> {
                    meterRegistry.counter("realtime.alerts.errors",
                                    "analysisId", analysisId.toString(),
                                    "error", ex.getClass().getSimpleName())
                            .increment();
                    log.error("Error triggering realtime alert for {}: {}", analysisId, ex.getMessage(), ex);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build());
                });
    }


    @Operation(summary = "Stream Realtime Metrics (SSE)",
            description = "Continuously stream predictive maintenance updates every 5 seconds")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE stream opened successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid analysisId")
    })
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PredictiveMaintenanceResponse>> streamRealtimeMetrics(
            @RequestParam("analysisId") @Min(1) Integer analysisId
    ) {
        log.debug("Request GET /stream?analysisId={}", analysisId);

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
