package com.banenor.controller;

import com.banenor.dto.PerformanceDTO;
import com.banenor.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@RestController
@RequestMapping(value = "/api/v1/performance", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Performance", description = "Endpoints for train performance metrics")
@PreAuthorize("hasRole('MAINTENANCE')")
@RequiredArgsConstructor
@Slf4j
public class PerformanceController {

    private final PerformanceService performanceService;

    /**
     * Retrieves performance metrics over a specified time range.
     * Defaults to the last 7 days if no dates are provided.
     */
    @Operation(summary = "Get Performance Data",
            description = "Fetch aggregated train performance metrics between startDate and endDate")
    @GetMapping
    public Flux<PerformanceDTO> getPerformanceData(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = (startDate != null ? startDate : now.minusDays(7));
        LocalDateTime to   = (endDate   != null ? endDate   : now);

        log.info("Fetching performance data from {} to {}", from, to);

        return performanceService.getPerformanceData(from, to)
                .doOnError(ex -> log.error("Error fetching performance data", ex))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
