package com.banenor.controller;

import com.banenor.dto.HistoricalDataResponse;
import com.banenor.dto.SensorMetricsDTO;
import com.banenor.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Endpoints for retrieving dashboard metrics")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Get Latest Metrics", description = "Retrieve the latest sensor metrics for a given analysis ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest metrics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis ID not found")
    })
    @GetMapping("/latest/{analysisId}")
    public Mono<ResponseEntity<SensorMetricsDTO>> getLatestMetrics(@PathVariable Integer analysisId) {
        return dashboardService.getLatestMetrics(analysisId)
                .map(ResponseEntity::ok)
                .doOnError(e -> logger.error("Error retrieving latest metrics for analysisId {}: {}", analysisId, e.getMessage()));
    }

    @Operation(summary = "Get Historical Data", description = "Retrieve historical sensor data for a given analysis ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historical data retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis ID not found")
    })
    @GetMapping("/historical/{analysisId}")
    public Mono<ResponseEntity<HistoricalDataResponse>> getHistoricalData(@PathVariable Integer analysisId) {
        return dashboardService.getHistoricalData(analysisId)
                .map(ResponseEntity::ok)
                .doOnError(e -> logger.error("Error retrieving historical data for analysisId {}: {}", analysisId, e.getMessage()));
    }
}
