package com.banenor.controller;

import com.banenor.dto.MaintenanceScheduleDTO;
import com.banenor.dto.PredictiveMaintenanceDTO;
import com.banenor.service.PredictiveMaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Predictive Maintenance", description = "Endpoints for predictive maintenance analysis and maintenance scheduling")
@Slf4j
@PreAuthorize("hasRole('MAINTENANCE')")
@RequiredArgsConstructor
public class MaintenancePredictionController {

    private final PredictiveMaintenanceService predictiveMaintenanceService;

    @Operation(summary = "Predict Maintenance", description = "Retrieve predictive maintenance insights for a given analysis ID with an optional alert email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance prediction retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis ID not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/predictive/{analysisId}")
    public Mono<ResponseEntity<PredictiveMaintenanceDTO>> predictMaintenance(
            @PathVariable @Min(1) Integer analysisId,
            @RequestParam(defaultValue = "alerts@example.com") @Email String alertEmail) {

        log.info("Predicting maintenance for analysisId={} with alertEmail={}", analysisId, alertEmail);

        return predictiveMaintenanceService.getMaintenanceAnalysis(analysisId, alertEmail)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .doOnError(ex -> log.error("Error predicting maintenance for analysisId={}", analysisId, ex))
                .onErrorResume(ex -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get Maintenance Schedule", description = "Retrieve upcoming maintenance tasks")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance schedule retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/maintenance/schedule")
    public Mono<ResponseEntity<List<MaintenanceScheduleDTO>>> getMaintenanceSchedule() {
        log.info("Fetching maintenance schedule");

        List<MaintenanceScheduleDTO> schedule = List.of(
                new MaintenanceScheduleDTO("2025-02-10T00:00:00.000+01:00", 1, "2025-02-10", "Wheel alignment for Car 2"),
                new MaintenanceScheduleDTO("2025-02-15T00:00:00.000+01:00", 2, "2025-02-15", "Brake inspection for Car 1")
        );

        return Mono.just(ResponseEntity.ok(schedule))
                .doOnSuccess(r -> log.debug("Maintenance schedule retrieved successfully"))
                .onErrorResume(ex -> {
                    log.error("Error retrieving maintenance schedule", ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
