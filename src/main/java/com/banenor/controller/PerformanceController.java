// src/main/java/com/banenor/controller/PerformanceController.java
package com.banenor.controller;

import com.banenor.dto.PerformanceDTO;
import com.banenor.dto.PerformanceRequest;
import com.banenor.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping(value = "/api/v1/performance", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Performance", description = "Endpoints for train performance metrics")
@PreAuthorize("hasRole('MAINTENANCE')")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PerformanceController {

    private final PerformanceService performanceService;

    @Operation(
            summary = "Get Performance Data",
            description = "Fetch aggregated train performance metrics between startDate and endDate"
    )
    @GetMapping
    public Flux<PerformanceDTO> getPerformanceData(@ModelAttribute @Validated PerformanceRequest req) {
        var from = req.getFrom();
        var to   = req.getTo();
        log.info("Fetching performance data from {} to {}", from, to);

        return performanceService.getPerformanceData(from, to)
                .doOnError(ex -> log.error("Error fetching performance data from {} to {}", from, to, ex))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
