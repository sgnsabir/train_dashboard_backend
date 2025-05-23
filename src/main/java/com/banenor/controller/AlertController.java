package com.banenor.controller;

import com.banenor.dto.AlertAcknowledgeRequest;
import com.banenor.dto.AlertDTO;
import com.banenor.dto.AlertStatsDTO;
import com.banenor.service.AlertService;
import com.banenor.websocket.WebSocketBroadcaster;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Endpoints for retrieving and acknowledging alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final WebSocketBroadcaster broadcaster;

    @Operation(
            summary = "Get Alert History",
            description = "Retrieve a list of past alerts, optionally filtered by trainNo and/or time range"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert history retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Flux<AlertDTO> getAlertHistory(
            @RequestParam(required = false) Integer trainNo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        log.info("Fetching alert history: trainNo={}, from={}, to={}", trainNo, from, to);

        return alertService.getAlertHistory(trainNo, from, to)
                .map(this::toDto)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(dto -> log.debug("Fetched alert DTO: {}", dto))
                .doOnError(ex -> log.error("Error retrieving alert history", ex))
                .onErrorResume(ex -> {
                    log.warn("Resuming with empty history due to error: {}", ex.getMessage());
                    return Flux.empty();
                });
    }

    @Operation(
            summary = "Get Alert Statistics",
            description = "Retrieve counts of alerts by severity over a time range"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert stats retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Mono<AlertStatsDTO> getAlertStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        log.info("Fetching alert stats: from={}, to={}", from, to);

        return alertService.getAlertHistory(null, from, to)
                .collectList()
                .map(list -> {
                    long info     = list.stream().filter(a -> a.getSeverity() == AlertDTO.Severity.INFO).count();
                    long warn     = list.stream().filter(a -> a.getSeverity() == AlertDTO.Severity.WARN).count();
                    long critical = list.stream().filter(a -> a.getSeverity() == AlertDTO.Severity.CRITICAL).count();
                    long total    = list.size();
                    AlertStatsDTO stats = new AlertStatsDTO(info, warn, critical, total, from, to);
                    log.debug("Computed alert stats: {}", stats);
                    return stats;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> log.error("Error retrieving alert stats", ex));
    }

    @Operation(
            summary = "Acknowledge Alert",
            description = "Mark a specific alert as acknowledged"
    )
    @ApiResponses({
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
            log.warn("Path id {} and payload id {} mismatch or missing",
                    id, request == null ? null : request.getAlertId());
            return Mono.just(ResponseEntity.badRequest().build());
        }

        log.info("Acknowledging alert id={}", id);
        return alertService.acknowledgeAlert(request)
                .then(Mono.fromSupplier(() -> {
                    log.debug("Successfully acknowledged alert id {}", id);

                    // Broadcast acknowledgment to WebSocket clients
                    AlertDTO ackDto = AlertDTO.builder()
                            .id(id)
                            .acknowledged(true)
                            .acknowledgedBy(request.getAcknowledgedBy())
                            .build();
                    broadcaster.publish(ackDto, "ALERT_ACK");
                    log.debug("Broadcasted ALERT_ACK for id {}", id);

                    return ResponseEntity.ok().<Void>build();
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> log.error("Error during acknowledgment of alert id={}", id, ex))
                .onErrorResume(ex -> {
                    log.error("Acknowledgment failed for alert id={}: {}", id, ex.getMessage());
                    return Mono.just(ResponseEntity.status(500).build());
                });
    }

    private AlertDTO toDto(com.banenor.dto.AlertResponse r) {
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
