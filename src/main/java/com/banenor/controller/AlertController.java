package com.banenor.controller;

import com.banenor.dto.AlertAcknowledgeRequest;
import com.banenor.dto.AlertResponse;
import com.banenor.service.AlertService;
import com.banenor.service.RealtimeAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Endpoints for retrieving, acknowledging, and triggering realtime alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final RealtimeAlertService realtimeAlertService;

    @Operation(summary = "Get Alert History", description = "Retrieve a list of past alerts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alert history retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public Flux<AlertResponse> getAlertHistory() {
        log.info("Fetching alert history");
        return alertService.getAlertHistory()
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(alert -> log.debug("Fetched alert: {}", alert))
                .doOnComplete(() -> log.info("Completed fetching alert history"))
                .doOnError(ex -> log.error("Error retrieving alert history: {}", ex.getMessage(), ex))
                .onErrorResume(ex -> {
                    log.error("Resuming with empty alert history due to error: {}", ex.getMessage(), ex);
                    return Flux.empty();
                });
    }

    @Operation(summary = "Acknowledge Alert", description = "Mark a specific alert as acknowledged")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alert acknowledged successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/acknowledge")
    public Mono<ResponseEntity<Object>> acknowledgeAlert(@RequestBody AlertAcknowledgeRequest request) {
        log.info("Acknowledging alert with id: {}", request.getAlertId());
        return alertService.acknowledgeAlert(request)
                .then(Mono.fromSupplier(() -> {
                    log.debug("Successfully acknowledged alert with id: {}", request.getAlertId());
                    // Return an OK response with a null body (Object type)
                    return ResponseEntity.ok(null);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> log.error("Error acknowledging alert with id {}: {}", request.getAlertId(), ex.getMessage(), ex))
                .onErrorResume(ex -> {
                    log.error("Returning internal server error for alert id {}: {}", request.getAlertId(), ex.getMessage(), ex);
                    // Return a 500 error response with a null body (Object type)
                    return Mono.just(ResponseEntity.status(500).body(null));
                });
    }

    @Operation(summary = "Trigger Realtime Alert",
            description = "Trigger a realtime alert for a given analysis ID. Optionally specify alertEmail to direct the alert.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Realtime alert triggered successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/alert/{analysisId}")
    public Mono<ResponseEntity<Object>> triggerRealtimeAlert(
            @PathVariable("analysisId") Integer analysisId,
            @RequestParam(name = "alertEmail", required = false, defaultValue = "alerts@example.com") String alertEmail) {
        log.info("Triggering realtime alert for analysisId: {} with alertEmail: {}", analysisId, alertEmail);
        return realtimeAlertService.monitorAndAlert(analysisId, alertEmail)
                .then(Mono.just(ResponseEntity.ok(null)))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("Error triggering realtime alert for analysisId {}: {}", analysisId, e.getMessage(), e));
    }
}
