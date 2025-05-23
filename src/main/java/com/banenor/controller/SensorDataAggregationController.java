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

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/aggregations")
@Tag(name = "Sensor Aggregations", description = "Endpoints for retrieving aggregated sensor metrics")
public class SensorDataAggregationController {

    private final AggregationService aggregationService;

    public SensorDataAggregationController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    //––– Speed & AOA –––//

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
        ).map(t -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageSpeed(t.getT1());
            dto.setMinSpeed(t.getT2());
            dto.setMaxSpeed(t.getT3());
            dto.setSpeedVariance(t.getT4());
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
        ).map(t -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageAoa(t.getT1());
            dto.setMinAoa(t.getT2());
            dto.setMaxAoa(t.getT3());
            dto.setAoaVariance(t.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    //––– Vibration –––//

    @Operation(summary = "Get Vibration Left Aggregations", description = "Retrieve left‐side vibration metrics")
    @ApiResponses(value = {
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
        ).map(t -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageVibrationLeft(t.getT1());
            dto.setMinVibrationLeft(t.getT2());
            dto.setMaxVibrationLeft(t.getT3());
            dto.setVibrationLeftVariance(t.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get Vibration Right Aggregations", description = "Retrieve right‐side vibration metrics")
    @ApiResponses(value = {
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
        ).map(t -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageVibrationRight(t.getT1());
            dto.setMinVibrationRight(t.getT2());
            dto.setMaxVibrationRight(t.getT3());
            dto.setVibrationRightVariance(t.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    //––– Vertical Force –––//

    @Operation(summary = "Get Vertical Force Left Aggregations", description = "Retrieve left‐side vertical force metrics")
    @ApiResponses(value = {
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
        ).map(t -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageVerticalForceLeft(t.getT1());
            dto.setMinVerticalForceLeft(t.getT2());
            dto.setMaxVerticalForceLeft(t.getT3());
            dto.setVerticalForceLeftVariance(t.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get Vertical Force Right Aggregations", description = "Retrieve right‐side vertical force metrics")
    @ApiResponses(value = {
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
        ).map(t -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageVerticalForceRight(t.getT1());
            dto.setMinVerticalForceRight(t.getT2());
            dto.setMaxVerticalForceRight(t.getT3());
            dto.setVerticalForceRightVariance(t.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    //––– Lateral Force –––//

    @Operation(summary = "Get Lateral Force Left Aggregations", description = "Retrieve left‐side lateral force metrics")
    @ApiResponses(value = {
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
        ).map(t -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageLateralForceLeft(t.getT1());
            dto.setMinLateralForceLeft(t.getT2());
            dto.setMaxLateralForceLeft(t.getT3());
            dto.setLateralForceLeftVariance(t.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get Lateral Force Right Aggregations", description = "Retrieve right‐side lateral force metrics")
    @ApiResponses(value = {
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
        ).map(t -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageLateralForceRight(t.getT1());
            dto.setMinLateralForceRight(t.getT2());
            dto.setMaxLateralForceRight(t.getT3());
            dto.setLateralForceRightVariance(t.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    //––– Lateral Vibration –––//

    @Operation(summary = "Get Lateral Vibration Left Aggregations", description = "Retrieve left‐side lateral vibration metrics")
    @ApiResponses(value = {
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
        ).map(t -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageLateralVibrationLeft(t.getT1());
            dto.setMinLateralVibrationLeft(t.getT2());
            dto.setMaxLateralVibrationLeft(t.getT3());
            dto.setLateralVibrationLeftVariance(t.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "Get Lateral Vibration Right Aggregations", description = "Retrieve right‐side lateral vibration metrics")
    @ApiResponses(value = {
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
        ).map(t -> {
            SensorAggregationDTO dto = new SensorAggregationDTO();
            dto.setAverageLateralVibrationRight(t.getT1());
            dto.setMinLateralVibrationRight(t.getT2());
            dto.setMaxLateralVibrationRight(t.getT3());
            dto.setLateralVibrationRightVariance(t.getT4());
            return ResponseEntity.ok(dto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    //––– All Aggregations (including both left & right where applicable) –––//

    @Operation(summary = "Get All Aggregations", description = "Retrieve consolidated aggregated metrics for all sensor types for a given analysis ID")
    @ApiResponses(value = {
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
