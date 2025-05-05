package com.banenor.controller;

import com.banenor.dto.AxlesDataDTO;
import com.banenor.dto.AxlesDataRequest;
import com.banenor.exception.ApiError;
import com.banenor.service.AxlesDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/axles")
@RequiredArgsConstructor
@Validated
@Tag(name = "Axles Data", description = "Stream and aggregate per-train axles sensor data")
public class AxlesDataController {

    private final AxlesDataService axlesDataService;

    @Operation(
            summary = "Stream axles data for a train over a time window",
            responses = {
                    @ApiResponse(responseCode = "200", description = "NDJSON stream of readings",
                            content = @Content(mediaType = "application/x-ndjson", schema = @Schema(implementation = AxlesDataDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @GetMapping(value = "/data", produces = "application/x-ndjson")
    public Flux<AxlesDataDTO> getAxlesData(
            @ParameterObject @Validated AxlesDataRequest request
    ) {
        log.info("GET /data {}", request);
        return axlesDataService.getAxlesData(
                request.getTrainNo(),
                request.getStart(),
                request.getEnd(),
                request.getMeasurementPoint()
        );
    }

    @Operation(
            summary = "Get global aggregated metrics for a measurement point",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Single aggregation DTO",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AxlesDataDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid measurementPoint", content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @GetMapping("/global-aggregations")
    public Mono<AxlesDataDTO> getGlobalAggregations(
            @Schema(description = "Measurement point (e.g. TP1)", example = "TP1", required = true)
            String measurementPoint
    ) {
        log.info("GET /global-aggregations measurementPoint={}", measurementPoint);
        return axlesDataService.getGlobalAggregations(measurementPoint);
    }
}
