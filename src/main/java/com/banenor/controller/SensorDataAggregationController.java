package com.banenor.controller;

import com.banenor.dto.SensorAggregationDTO;
import com.banenor.service.AggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/v1/aggregations")
@Tag(name = "Sensor Aggregations", description = "Endpoints for retrieving aggregated sensor metrics")
public class SensorDataAggregationController {

    private final AggregationService aggregationService;

    public SensorDataAggregationController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @Operation(summary = "Get Speed Aggregations", description = "Retrieve aggregated speed metrics for a given analysis ID")
    @ApiResponses(value = {
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
    @ApiResponses(value = {
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

    @Operation(summary = "Get Vibration Aggregations", description = "Retrieve aggregated vibration metrics for a given analysis ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vibration aggregations retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{analysisId}/vibration")
    public Mono<ResponseEntity<SensorAggregationDTO>> getVibrationAggregations(@PathVariable Integer analysisId) {
        return Mono.zip(
                aggregationService.getAverageVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getVibrationLeftVariance(analysisId).defaultIfEmpty(0.0)
        ).map(tuple -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageVibration(tuple.getT1());
            dto.setMinVibration(tuple.getT2());
            dto.setMaxVibration(tuple.getT3());
            dto.setVibrationVariance(tuple.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get Vertical Force Left Aggregations", description = "Retrieve aggregated vertical force left metrics for a given analysis ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vertical force left aggregations retrieved successfully"),
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

    @Operation(summary = "Get Lateral Force Left Aggregations", description = "Retrieve aggregated lateral force left metrics for a given analysis ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lateral force left aggregations retrieved successfully"),
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

    @Operation(summary = "Get Lateral Vibration Left Aggregations", description = "Retrieve aggregated lateral vibration left metrics for a given analysis ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lateral vibration left aggregations retrieved successfully"),
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

    @Operation(summary = "Get All Aggregations", description = "Retrieve consolidated aggregated metrics for all sensor types for a given analysis ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consolidated aggregations retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{analysisId}/all")
    public Mono<ResponseEntity<SensorAggregationDTO>> getAllAggregations(@PathVariable Integer analysisId) {
        List<Mono<Double>> sources = List.of(
                aggregationService.getAverageSpeed(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinSpeed(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxSpeed(analysisId).defaultIfEmpty(0.0),
                aggregationService.getSpeedVariance(analysisId).defaultIfEmpty(0.0),
                aggregationService.getAverageAoa(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinAoa(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxAoa(analysisId).defaultIfEmpty(0.0),
                aggregationService.getAoaVariance(analysisId).defaultIfEmpty(0.0),
                aggregationService.getAverageVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getVibrationLeftVariance(analysisId).defaultIfEmpty(0.0),
                aggregationService.getAverageVerticalForceLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinVerticalForceLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxVerticalForceLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getVerticalForceLeftVariance(analysisId).defaultIfEmpty(0.0),
                aggregationService.getAverageLateralForceLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinLateralForceLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxLateralForceLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getLateralForceLeftVariance(analysisId).defaultIfEmpty(0.0),
                aggregationService.getAverageLateralVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMinLateralVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getMaxLateralVibrationLeft(analysisId).defaultIfEmpty(0.0),
                aggregationService.getLateralVibrationLeftVariance(analysisId).defaultIfEmpty(0.0)
        );
        return Mono.zip(sources, results -> {
            Object[] arr = results;
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageSpeed((Double) arr[0]);
            dto.setMinSpeed((Double) arr[1]);
            dto.setMaxSpeed((Double) arr[2]);
            dto.setSpeedVariance((Double) arr[3]);
            dto.setAverageAoa((Double) arr[4]);
            dto.setMinAoa((Double) arr[5]);
            dto.setMaxAoa((Double) arr[6]);
            dto.setAoaVariance((Double) arr[7]);
            dto.setAverageVibration((Double) arr[8]);
            dto.setMinVibration((Double) arr[9]);
            dto.setMaxVibration((Double) arr[10]);
            dto.setVibrationVariance((Double) arr[11]);
            dto.setAverageVerticalForceLeft((Double) arr[12]);
            dto.setMinVerticalForceLeft((Double) arr[13]);
            dto.setMaxVerticalForceLeft((Double) arr[14]);
            dto.setVerticalForceLeftVariance((Double) arr[15]);
            dto.setAverageLateralForceLeft((Double) arr[16]);
            dto.setMinLateralForceLeft((Double) arr[17]);
            dto.setMaxLateralForceLeft((Double) arr[18]);
            dto.setLateralForceLeftVariance((Double) arr[19]);
            dto.setAverageLateralVibrationLeft((Double) arr[20]);
            dto.setMinLateralVibrationLeft((Double) arr[21]);
            dto.setMaxLateralVibrationLeft((Double) arr[22]);
            dto.setLateralVibrationLeftVariance((Double) arr[23]);
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
