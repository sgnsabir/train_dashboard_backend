package com.banenor.controller;

import com.banenor.dto.ReliabilityHealthDTO;
import com.banenor.service.ReliabilityHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping(
        value = "/api/v1/reliability-health",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@Tag(name = "Reliability Health", description = "Endpoints for calculating train reliability health scores")
@PreAuthorize("hasRole('MAINTENANCE')")
@Validated
@Slf4j
@RequiredArgsConstructor
public class ReliabilityHealthController {

    private final ReliabilityHealthService reliabilityHealthService;

    @Operation(
            summary = "Get Reliability Health",
            description = "Calculate the overall reliability health score for a specified train based on the latest sensor metrics."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reliability health retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid train number supplied"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public Mono<ResponseEntity<ReliabilityHealthDTO>> getReliabilityHealth(
            @RequestParam("trainNo") @Min(1) Integer trainNo) {

        log.info("Requesting reliability health for trainNo={}", trainNo);

        return reliabilityHealthService.calculateHealthScore(trainNo)
                .map(ResponseEntity::ok)
                .doOnError(ex ->
                        log.error("Error fetching reliability health for trainNo={}: {}", trainNo, ex.getMessage(), ex)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }
}
