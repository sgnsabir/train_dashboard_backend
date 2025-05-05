package com.banenor.controller;

import com.banenor.dto.RawDataResponse;
import com.banenor.dto.SensorMeasurementDTO;
import com.banenor.exception.ResourceNotFoundException;
import com.banenor.model.AbstractHeader;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.repository.HaugfjellMP3HeaderRepository;
import com.banenor.service.DataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/data")
@Tag(name = "Data", description = "Endpoint for raw and detailed sensor data retrieval")
@Validated
@Slf4j
@RequiredArgsConstructor
public class DataController {

    private final DataService dataService;
    private final HaugfjellMP1HeaderRepository mp1HeaderRepo;
    private final HaugfjellMP3HeaderRepository mp3HeaderRepo;

    private Mono<AbstractHeader> findHeader(@PathVariable @Min(1) Integer analysisId) {
        return Mono.firstWithValue(
                        mp1HeaderRepo.findById(analysisId).cast(AbstractHeader.class),
                        mp3HeaderRepo.findById(analysisId).cast(AbstractHeader.class)
                )
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Header not found for analysis ID {}", analysisId);
                    return Mono.error(new ResourceNotFoundException("Analysis ID " + analysisId + " not found."));
                }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(
            summary = "Get Raw Sensor Data",
            description = "Retrieve paginated raw sensor data for a given analysis ID with optional sensor type filtering"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Raw sensor data retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis ID not found"),
            @ApiResponse(responseCode = "400", description = "Invalid paging parameters")
    })
    @GetMapping("/raw/{analysisId}")
    public Mono<ResponseEntity<Flux<RawDataResponse>>> getRawSensorData(
            @PathVariable @Min(1) Integer analysisId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(0) int size,
            @RequestParam(required = false) String sensorType
    ) {
        return findHeader(analysisId)
                .map(header -> {
                    log.debug("Streaming raw data for analysisId={}, sensorType={}, page={}, size={}",
                            analysisId, sensorType, page, size);
                    Flux<RawDataResponse> stream = dataService
                            .getRawData(analysisId, sensorType, page, size)
                            .subscribeOn(Schedulers.boundedElastic());
                    return ResponseEntity.ok(stream);
                })
                .onErrorResume(ResourceNotFoundException.class, ex -> {
                    log.warn("Raw data request failed: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @Operation(
            summary = "Get Detailed Sensor Measurements",
            description = "Retrieve paginated detailed sensor measurements for a given analysis ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detailed sensor measurements retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis ID not found"),
            @ApiResponse(responseCode = "400", description = "Invalid paging parameters")
    })
    @GetMapping("/detailed/{analysisId}")
    public Mono<ResponseEntity<Flux<SensorMeasurementDTO>>> getDetailedSensorData(
            @PathVariable @Min(1) Integer analysisId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(0) int size
    ) {
        return findHeader(analysisId)
                .map(header -> {
                    log.debug("Streaming detailed data for analysisId={}, page={}, size={}", analysisId, page, size);
                    Flux<SensorMeasurementDTO> stream = dataService
                            .getDetailedSensorData(analysisId, page, size)
                            .subscribeOn(Schedulers.boundedElastic());
                    return ResponseEntity.ok(stream);
                })
                .onErrorResume(ResourceNotFoundException.class, ex -> {
                    log.warn("Detailed data request failed: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }
}
