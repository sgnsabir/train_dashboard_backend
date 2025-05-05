package com.banenor.controller;

import com.banenor.dto.AlertAcknowledgeRequest;
import com.banenor.dto.AlertDTO;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Flux<AlertDTO> getAlertHistory() {
        log.info("Fetching alert history");
        return alertService.getAlertHistory()
                .map(this::toDto)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(dto -> log.debug("Fetched alert DTO: {}", dto))
                .doOnComplete(() -> log.info("Completed fetching alert history"))
                .doOnError(ex -> log.error("Error retrieving alert history: {}", ex.getMessage(), ex))
                .onErrorResume(ex -> {
                    // fallback to empty list on error
                    log.warn("Resuming with empty history due to error");
                    return Flux.empty();
                });
    }

    @Operation(summary = "Acknowledge Alert", description = "Mark a specific alert as acknowledged")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alert acknowledged successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or ID mismatch"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Mono<ResponseEntity<Void>> acknowledgeAlert(
            @PathVariable("id") Long id,
            @RequestBody AlertAcknowledgeRequest request
    ) {
        if (request == null || request.getAlertId() == null || !id.equals(request.getAlertId())) {
            log.warn("Path id {} and payload id {} mismatch or missing", id,
                    request == null ? null : request.getAlertId());
            return Mono.just(ResponseEntity.badRequest().build());
        }

        log.info("Acknowledging alert with id: {}", id);
        return alertService.acknowledgeAlert(request)
                .then(Mono.fromSupplier(() -> {
                    log.debug("Successfully acknowledged alert id {}", id);
                    return ResponseEntity.ok().<Void>build();
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> log.error("Error acknowledging alert id {}: {}", id, ex.getMessage(), ex))
                .onErrorResume(ex ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
                );
    }

    /**
     * Helper to convert internal AlertResponse into the richer AlertDTO for clients.
     */
    private AlertDTO toDto(AlertResponse r) {
        return AlertDTO.builder()
                .id(r.getId())
                .subject(r.getSubject())
                .message(r.getMessage())
                .timestamp(r.getTimestamp())
                .severity(r.getSeverity())
                .trainNo(r.getTrainNo())
                .acknowledged(r.getAcknowledged())
                .acknowledgedBy(r.getAcknowledgedBy())
                .build();
    }
}
