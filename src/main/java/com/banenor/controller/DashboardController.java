package com.banenor.controller;

import com.banenor.dto.HistoricalDataResponse;
import com.banenor.dto.SensorMetricsDTO;
import com.banenor.dto.SystemDashboardDTO;
import com.banenor.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Endpoints for retrieving dashboard metrics")
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Get Latest Metrics",
            description = "Retrieve the latest sensor metrics for a given analysis ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Latest metrics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis ID not found")
    })
    @GetMapping("/latest/{analysisId}")
    public Mono<ResponseEntity<SensorMetricsDTO>> getLatestMetrics(
            @PathVariable Integer analysisId
    ) {
        return dashboardService.getLatestMetrics(analysisId)
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Error retrieving latest metrics for {}: {}", analysisId, e.getMessage()));
    }

    @Operation(summary = "Get Historical Data",
            description = "Retrieve historical sensor data for a given analysis ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historical data retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis ID not found")
    })
    @GetMapping("/historical/{analysisId}")
    public Mono<ResponseEntity<HistoricalDataResponse>> getHistoricalData(
            @PathVariable Integer analysisId
    ) {
        return dashboardService.getHistoricalData(analysisId)
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Error retrieving historical data for {}: {}", analysisId, e.getMessage()));
    }

    @Operation(summary = "Get System Dashboard",
            description = "Retrieve global fleet metrics, recent alerts, and system health")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "System dashboard retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/global")
    public Mono<ResponseEntity<SystemDashboardDTO>> getSystemDashboard() {
        log.info("Fetching global system dashboard");
        return dashboardService.getSystemDashboard()
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Error retrieving system dashboard: {}", e.getMessage()));
    }
}
