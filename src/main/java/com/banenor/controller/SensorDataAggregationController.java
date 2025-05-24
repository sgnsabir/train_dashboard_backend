package com.banenor.controller;

import com.banenor.dto.SensorAggregationDTO;
import com.banenor.service.AggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/aggregations")
@Tag(name = "Sensor Aggregations", description = "Endpoints for retrieving aggregated sensor metrics and triggering aggregations")
@Slf4j
@RequiredArgsConstructor
public class SensorDataAggregationController {

    private final AggregationService aggregationService;

    //───────────────────────────────────────────────────────────────────────────────
    // 0) Trigger sensor-data aggregation over a time-range
    //───────────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Trigger aggregated sensor metrics",
            description = "Compute (and persist/cache) aggregated sensor metrics over the given time-range"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aggregations triggered successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public Mono<ResponseEntity<Void>> triggerAggregations(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        log.info("GET /api/v1/aggregations?from={} → to={}", from, to);
        return aggregationService.aggregateSensorDataByRange(from, to)
                .then(Mono.fromSupplier(() -> {
                    log.info("← Aggregations complete for range {} → {}", from, to);
                    return ResponseEntity.ok().<Void>build();
                }))
                .onErrorResume(err -> {
                    log.error("Aggregation failed for range {} → {}", from, to, err);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    //───────────────────────────────────────────────────────────────────────────────
    // 1) Individual sensor‐type aggregations per analysisId
    //───────────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Get Speed Aggregations", description = "Retrieve aggregated speed metrics for a given analysis ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Speed aggregations retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{analysisId}/speed")
    public Mono<ResponseEntity<SensorAggregationDTO>> getSpeedAggregations(@PathVariable Integer analysisId) {
        return Mono.zip(
                aggregationService.getAverageSpeed(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinSpeed(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxSpeed(analysisId).defaultIfEmpty(0.0),
                aggregationService.getSpeedVariance(analysisId).defaultIfEmpty(0.0)
        ).map(tuple -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageSpeed(tuple.getT1());
            dto.setMinSpeed(tuple.getT2());
            dto.setMaxSpeed(tuple.getT3());
            dto.setSpeedVariance(tuple.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get AOA Aggregations", description = "Retrieve aggregated Angle of Attack (AOA) metrics for a given analysis ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AOA aggregations retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{analysisId}/aoa")
    public Mono<ResponseEntity<SensorAggregationDTO>> getAoaAggregations(@PathVariable Integer analysisId) {
        return Mono.zip(
                aggregationService.getAverageAoa(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinAoa(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxAoa(analysisId).defaultIfEmpty(0.0),
                aggregationService.getAoaVariance(analysisId).defaultIfEmpty(0.0)
        ).map(tuple -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageAoa(tuple.getT1());
            dto.setMinAoa(tuple.getT2());
            dto.setMaxAoa(tuple.getT3());
            dto.setAoaVariance(tuple.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get Vibration Left Aggregations", description = "Retrieve left‐side vibration metrics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Left vibration aggregations retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{analysisId}/vibration-left")
    public Mono<ResponseEntity<SensorAggregationDTO>> getVibrationLeftAggregations(@PathVariable Integer analysisId) {
        return Mono.zip(
                aggregationService.getAverageVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getVibrationLeftVariance(analysisId).defaultIfEmpty(0.0)
        ).map(tuple -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageVibrationLeft(tuple.getT1());
            dto.setMinVibrationLeft(tuple.getT2());
            dto.setMaxVibrationLeft(tuple.getT3());
            dto.setVibrationLeftVariance(tuple.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get Vibration Right Aggregations", description = "Retrieve right‐side vibration metrics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Right vibration aggregations retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{analysisId}/vibration-right")
    public Mono<ResponseEntity<SensorAggregationDTO>> getVibrationRightAggregations(@PathVariable Integer analysisId) {
        return Mono.zip(
                aggregationService.getAverageVibrationRight(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinVibrationRight(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxVibrationRight(analysisId).defaultIfEmpty(0.0),
                aggregationService.getVibrationRightVariance(analysisId).defaultIfEmpty(0.0)
        ).map(tuple -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageVibrationRight(tuple.getT1());
            dto.setMinVibrationRight(tuple.getT2());
            dto.setMaxVibrationRight(tuple.getT3());
            dto.setVibrationRightVariance(tuple.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get Vertical Force Left Aggregations", description = "Retrieve left‐side vertical force metrics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Left vertical force aggregations retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{analysisId}/vertical-force-left")
    public Mono<ResponseEntity<SensorAggregationDTO>> getVerticalForceLeftAggregations(@PathVariable Integer analysisId) {
        return Mono.zip(
                aggregationService.getAverageVerticalForceLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinVerticalForceLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxVerticalForceLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getVerticalForceLeftVariance(analysisId).defaultIfEmpty(0.0)
        ).map(tuple -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageVerticalForceLeft(tuple.getT1());
            dto.setMinVerticalForceLeft(tuple.getT2());
            dto.setMaxVerticalForceLeft(tuple.getT3());
            dto.setVerticalForceLeftVariance(tuple.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get Vertical Force Right Aggregations", description = "Retrieve right‐side vertical force metrics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Right vertical force aggregations retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{analysisId}/vertical-force-right")
    public Mono<ResponseEntity<SensorAggregationDTO>> getVerticalForceRightAggregations(@PathVariable Integer analysisId) {
        return Mono.zip(
                aggregationService.getAverageVerticalForceRight(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinVerticalForceRight(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxVerticalForceRight(analysisId).defaultIfEmpty(0.0),
                aggregationService.getVerticalForceRightVariance(analysisId).defaultIfEmpty(0.0)
        ).map(tuple -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageVerticalForceRight(tuple.getT1());
            dto.setMinVerticalForceRight(tuple.getT2());
            dto.setMaxVerticalForceRight(tuple.getT3());
            dto.setVerticalForceRightVariance(tuple.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get Lateral Force Left Aggregations", description = "Retrieve left‐side lateral force metrics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Left lateral force aggregations retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{analysisId}/lateral-force-left")
    public Mono<ResponseEntity<SensorAggregationDTO>> getLateralForceLeftAggregations(@PathVariable Integer analysisId) {
        return Mono.zip(
                aggregationService.getAverageLateralForceLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinLateralForceLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxLateralForceLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getLateralForceLeftVariance(analysisId).defaultIfEmpty(0.0)
        ).map(tuple -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageLateralForceLeft(tuple.getT1());
            dto.setMinLateralForceLeft(tuple.getT2());
            dto.setMaxLateralForceLeft(tuple.getT3());
            dto.setLateralForceLeftVariance(tuple.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get Lateral Force Right Aggregations", description = "Retrieve right‐side lateral force metrics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Right lateral force aggregations retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{analysisId}/lateral-force-right")
    public Mono<ResponseEntity<SensorAggregationDTO>> getLateralForceRightAggregations(@PathVariable Integer analysisId) {
        return Mono.zip(
                aggregationService.getAverageLateralForceRight(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinLateralForceRight(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxLateralForceRight(analysisId).defaultIfEmpty(0.0),
                aggregationService.getLateralForceRightVariance(analysisId).defaultIfEmpty(0.0)
        ).map(tuple -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageLateralForceRight(tuple.getT1());
            dto.setMinLateralForceRight(tuple.getT2());
            dto.setMaxLateralForceRight(tuple.getT3());
            dto.setLateralForceRightVariance(tuple.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get Lateral Vibration Left Aggregations", description = "Retrieve left‐side lateral vibration metrics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Left lateral vibration aggregations retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{analysisId}/lateral-vibration-left")
    public Mono<ResponseEntity<SensorAggregationDTO>> getLateralVibrationLeftAggregations(@PathVariable Integer analysisId) {
        return Mono.zip(
                aggregationService.getAverageLateralVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinLateralVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxLateralVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getLateralVibrationLeftVariance(analysisId).defaultIfEmpty(0.0)
        ).map(tuple -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageLateralVibrationLeft(tuple.getT1());
            dto.setMinLateralVibrationLeft(tuple.getT2());
            dto.setMaxLateralVibrationLeft(tuple.getT3());
            dto.setLateralVibrationLeftVariance(tuple.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get Lateral Vibration Right Aggregations", description = "Retrieve right‐side lateral vibration metrics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Right lateral vibration aggregations retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{analysisId}/lateral-vibration-right")
    public Mono<ResponseEntity<SensorAggregationDTO>> getLateralVibrationRightAggregations(@PathVariable Integer analysisId) {
        return Mono.zip(
                aggregationService.getAverageLateralVibrationRight(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinLateralVibrationRight(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxLateralVibrationRight(analysisId).defaultIfEmpty(0.0),
                aggregationService.getLateralVibrationRightVariance(analysisId).defaultIfEmpty(0.0)
        ).map(tuple -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageLateralVibrationRight(tuple.getT1());
            dto.setMinLateralVibrationRight(tuple.getT2());
            dto.setMaxLateralVibrationRight(tuple.getT3());
            dto.setLateralVibrationRightVariance(tuple.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    //───────────────────────────────────────────────────────────────────────────────
    // 2) Consolidated “all” endpoint
    //───────────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Get All Aggregations", description = "Retrieve consolidated aggregated metrics for all sensor types for a given analysis ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consolidated aggregations retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{analysisId}/all")
    public Mono<ResponseEntity<SensorAggregationDTO>> getAllAggregations(@PathVariable Integer analysisId) {
        List<Mono<Double>> sources = new ArrayList<>();

        // Speed & AOA
        sources.add(aggregationService.getAverageSpeed(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMinSpeed(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMaxSpeed(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getSpeedVariance(analysisId).defaultIfEmpty(0.0));

        sources.add(aggregationService.getAverageAoa(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMinAoa(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMaxAoa(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getAoaVariance(analysisId).defaultIfEmpty(0.0));

        // Vibration L & R
        sources.add(aggregationService.getAverageVibrationLeft(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMinVibrationLeft(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMaxVibrationLeft(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getVibrationLeftVariance(analysisId).defaultIfEmpty(0.0));

        sources.add(aggregationService.getAverageVibrationRight(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMinVibrationRight(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMaxVibrationRight(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getVibrationRightVariance(analysisId).defaultIfEmpty(0.0));

        // Vertical Force L & R
        sources.add(aggregationService.getAverageVerticalForceLeft(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMinVerticalForceLeft(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMaxVerticalForceLeft(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getVerticalForceLeftVariance(analysisId).defaultIfEmpty(0.0));

        sources.add(aggregationService.getAverageVerticalForceRight(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMinVerticalForceRight(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMaxVerticalForceRight(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getVerticalForceRightVariance(analysisId).defaultIfEmpty(0.0));

        // Lateral Force L & R
        sources.add(aggregationService.getAverageLateralForceLeft(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMinLateralForceLeft(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMaxLateralForceLeft(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getLateralForceLeftVariance(analysisId).defaultIfEmpty(0.0));

        sources.add(aggregationService.getAverageLateralForceRight(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMinLateralForceRight(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMaxLateralForceRight(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getLateralForceRightVariance(analysisId).defaultIfEmpty(0.0));

        // Lateral Vibration L & R
        sources.add(aggregationService.getAverageLateralVibrationLeft(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMinLateralVibrationLeft(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMaxLateralVibrationLeft(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getLateralVibrationLeftVariance(analysisId).defaultIfEmpty(0.0));

        sources.add(aggregationService.getAverageLateralVibrationRight(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMinLateralVibrationRight(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getMaxLateralVibrationRight(analysisId).defaultIfEmpty(0.0));
        sources.add(aggregationService.getLateralVibrationRightVariance(analysisId).defaultIfEmpty(0.0));

        return Mono.zip(sources, results -> {
            Object[] r = results;
            SensorAggregationDTO dto = new SensorAggregationDTO();
            int i = 0;

            dto.setAverageSpeed((Double) r[i++]);
            dto.setMinSpeed((Double) r[i++]);
            dto.setMaxSpeed((Double) r[i++]);
            dto.setSpeedVariance((Double) r[i++]);

            dto.setAverageAoa((Double) r[i++]);
            dto.setMinAoa((Double) r[i++]);
            dto.setMaxAoa((Double) r[i++]);
            dto.setAoaVariance((Double) r[i++]);

            dto.setAverageVibrationLeft((Double) r[i++]);
            dto.setMinVibrationLeft((Double) r[i++]);
            dto.setMaxVibrationLeft((Double) r[i++]);
            dto.setVibrationLeftVariance((Double) r[i++]);

            dto.setAverageVibrationRight((Double) r[i++]);
            dto.setMinVibrationRight((Double) r[i++]);
            dto.setMaxVibrationRight((Double) r[i++]);
            dto.setVibrationRightVariance((Double) r[i++]);

            dto.setAverageVerticalForceLeft((Double) r[i++]);
            dto.setMinVerticalForceLeft((Double) r[i++]);
            dto.setMaxVerticalForceLeft((Double) r[i++]);
            dto.setVerticalForceLeftVariance((Double) r[i++]);

            dto.setAverageVerticalForceRight((Double) r[i++]);
            dto.setMinVerticalForceRight((Double) r[i++]);
            dto.setMaxVerticalForceRight((Double) r[i++]);
            dto.setVerticalForceRightVariance((Double) r[i++]);

            dto.setAverageLateralForceLeft((Double) r[i++]);
            dto.setMinLateralForceLeft((Double) r[i++]);
            dto.setMaxLateralForceLeft((Double) r[i++]);
            dto.setLateralForceLeftVariance((Double) r[i++]);

            dto.setAverageLateralForceRight((Double) r[i++]);
            dto.setMinLateralForceRight((Double) r[i++]);
            dto.setMaxLateralForceRight((Double) r[i++]);
            dto.setLateralForceRightVariance((Double) r[i++]);

            dto.setAverageLateralVibrationLeft((Double) r[i++]);
            dto.setMinLateralVibrationLeft((Double) r[i++]);
            dto.setMaxLateralVibrationLeft((Double) r[i++]);
            dto.setLateralVibrationLeftVariance((Double) r[i++]);

            dto.setAverageLateralVibrationRight((Double) r[i++]);
            dto.setMinLateralVibrationRight((Double) r[i++]);
            dto.setMaxLateralVibrationRight((Double) r[i++]);
            dto.setLateralVibrationRightVariance((Double) r[i++]);

            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
