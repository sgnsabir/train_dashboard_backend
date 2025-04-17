package com.banenor.controller;

import com.banenor.dto.AlertAcknowledgeRequest;
import com.banenor.dto.AlertResponse;
import com.banenor.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Endpoints for retrieving and acknowledging alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

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
    public Mono<ResponseEntity<Void>> acknowledgeAlert(@RequestBody AlertAcknowledgeRequest request) {
        log.info("Acknowledging alert with id: {}", request.getAlertId());
        return alertService.acknowledgeAlert(request)
                .then(Mono.fromSupplier(() -> {
                    log.debug("Successfully acknowledged alert with id: {}", request.getAlertId());
                    return ResponseEntity.ok().<Void>build();
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> log.error("Error acknowledging alert with id {}: {}", request.getAlertId(), ex.getMessage(), ex))
                .onErrorResume(ex -> {
                    log.error("Returning internal server error for alert id {}: {}", request.getAlertId(), ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * @deprecated Use POST /api/v1/realtime/alert/{analysisId} instead.
     */
    @Deprecated
    @PostMapping("/alert/{analysisId}")
    public Mono<ResponseEntity<Void>> redirectRealtimeAlert(@PathVariable Integer analysisId) {
        URI newLocation = URI.create(String.format("/api/v1/realtime/alert/%d", analysisId));
        log.warn("Deprecated endpoint called: redirecting to {}", newLocation);
        return Mono.just(ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(newLocation)
                .build());
    }
}
