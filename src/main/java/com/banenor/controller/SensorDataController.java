package com.banenor.controller;

import com.banenor.dto.AggregatedMetricsResponse;
import com.banenor.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/sensor")
@Tag(name = "Sensor Data", description = "Endpoint for cached aggregated sensor metrics")
public class SensorDataController {

    private final CacheService cacheService;

    public SensorDataController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Operation(summary = "Get Cached Sensor Averages", description = "Fetch aggregated sensor metrics (e.g., average speed) from cache")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cached sensor data retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/averages")
    public Mono<ResponseEntity<AggregatedMetricsResponse>> getSensorAverages() {
        return cacheService.getCachedAverage("avgSpeed")
                .flatMap(avg -> {
                    AggregatedMetricsResponse response = new AggregatedMetricsResponse(Mono.just(avg));
                    return Mono.just(ResponseEntity.ok(response));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.ok(new AggregatedMetricsResponse("No cached average available"))))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
