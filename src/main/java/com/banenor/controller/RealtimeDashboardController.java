package com.banenor.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Validated
@RestController
@RequestMapping("/api/v1/realtime")
@Tag(name = "Realtime Dashboard", description = "Endpoints for realtime metrics and alert triggering")
@SecurityRequirement(name = "bearerAuth")
public class RealtimeDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeDashboardController.class);
    private static final String DEFAULT_ALERT_EMAIL = "alerts@example.com";

    private final RealtimeAlertService realtimeAlertService;
    private final PredictiveMaintenanceService predictiveMaintenanceService;
    private final Optional<SimpMessagingTemplate> messagingTemplate;
    private final MeterRegistry meterRegistry;

    public RealtimeDashboardController(
            RealtimeAlertService realtimeAlertService,
            PredictiveMaintenanceService predictiveMaintenanceService,
            Optional<SimpMessagingTemplate> messagingTemplate,
            MeterRegistry meterRegistry) {
        this.realtimeAlertService = realtimeAlertService;
        this.predictiveMaintenanceService = predictiveMaintenanceService;
        this.messagingTemplate = messagingTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Operation(
        summary = "Get Latest Realtime Metrics",
        description = "Fetch the latest predictive maintenance analysis including aggregated metrics and risk assessment"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Realtime metrics retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid analysis ID"),
        @ApiResponse(responseCode = "404", description = "Analysis ID not found"),
        @ApiResponse(responseCode = "429", description = "Too many requests"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/metrics/{analysisId}")
    public Mono<ResponseEntity<PredictiveMaintenanceDTO>> getLatestMetrics(
            @Parameter(description = "Analysis ID to fetch metrics for", required = true)
            @PathVariable @Min(1) Integer analysisId) {
        
        return Mono.defer(() -> {
            meterRegistry.counter("realtime.metrics.requests", "analysisId", analysisId.toString()).increment();
            
            return predictiveMaintenanceService.getMaintenanceAnalysis(analysisId, DEFAULT_ALERT_EMAIL)
                    .map(ResponseEntity::ok)
                    .doOnSuccess(response -> 
                        meterRegistry.counter("realtime.metrics.success", "analysisId", analysisId.toString()).increment())
                    .doOnError(ex -> {
                        meterRegistry.counter("realtime.metrics.errors", 
                            "analysisId", analysisId.toString(),
                            "error", ex.getClass().getSimpleName()).increment();
                        logger.error("Error retrieving realtime metrics for analysisId {}: {}", analysisId, ex.getMessage());
                    })
                    .subscribeOn(Schedulers.boundedElastic());
        });
    }

    @Operation(
        summary = "Trigger Realtime Alert",
        description = "Trigger realtime alert evaluation and notify subscribers via WebSocket"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Realtime alert triggered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
        @ApiResponse(responseCode = "429", description = "Too many requests"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/alert/{analysisId}")
    public Mono<ResponseEntity<Void>> triggerAlert(
            @Parameter(description = "Analysis ID to trigger alert for", required = true)
            @PathVariable @Min(1) Integer analysisId,
            @Parameter(description = "Email address to send alerts to")
            @RequestParam(defaultValue = DEFAULT_ALERT_EMAIL) @Email String alertEmail) {

        return Mono.defer(() -> {
            meterRegistry.counter("realtime.alerts.requests", 
                "analysisId", analysisId.toString(),
                "email", alertEmail).increment();

            return realtimeAlertService.monitorAndAlert(analysisId, alertEmail)
                    .doOnSuccess(v -> {
                        meterRegistry.counter("realtime.alerts.success", 
                            "analysisId", analysisId.toString()).increment();
                        
                        if (messagingTemplate.isPresent()) {
                            messagingTemplate.get().convertAndSend("/topic/alerts",
                                    "Realtime alert processed for analysisId: " + analysisId);
                        } else {
                            logger.warn("SimpMessagingTemplate bean not available; WebSocket notification skipped for analysisId: {}", 
                                analysisId);
                        }
                    })
                    .then(Mono.<ResponseEntity<Void>>just(ResponseEntity.ok().build()))
                    .doOnError(ex -> {
                        meterRegistry.counter("realtime.alerts.errors",
                            "analysisId", analysisId.toString(),
                            "error", ex.getClass().getSimpleName()).increment();
                        logger.error("Error triggering realtime alert for analysisId {}: {}", analysisId, ex.getMessage());
                    })
                    .subscribeOn(Schedulers.boundedElastic());
        });
    }
}
