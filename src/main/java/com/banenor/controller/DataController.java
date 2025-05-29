package com.banenor.controller;

import com.banenor.dto.RawDataRequest;
import com.banenor.dto.RawDataResponse;
import com.banenor.dto.SensorTpSeriesDTO;
import com.banenor.exception.ResourceNotFoundException;
import com.banenor.model.AbstractHeader;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.repository.HaugfjellMP3HeaderRepository;
import com.banenor.service.DataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/data")
@Tag(name = "Data", description = "Endpoints for raw, detailed, and TP-series sensor data")
@Validated
@Slf4j
@RequiredArgsConstructor
public class DataController {

    private final DataService dataService;
    private final HaugfjellMP1HeaderRepository mp1HeaderRepo;
    private final HaugfjellMP3HeaderRepository mp3HeaderRepo;

    private Mono<AbstractHeader> findHeader(Integer analysisId) {
        if (analysisId == null) {
            // no header check when station=BOTH without analysisId
            return Mono.empty();
        }
        return Mono.firstWithValue(
                        mp1HeaderRepo.findById(analysisId).cast(AbstractHeader.class),
                        mp3HeaderRepo.findById(analysisId).cast(AbstractHeader.class)
                )
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Header not found for analysis ID {}", analysisId);
                    return Mono.error(new ResourceNotFoundException(
                            "Analysis ID " + analysisId + " not found."
                    ));
                }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(
            summary     = "Get Raw Sensor Data",
            description = "Retrieve paginated raw sensor data for a given analysis ID with optional sensor type filtering"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Raw sensor data retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis ID not found"),
            @ApiResponse(responseCode = "400", description = "Invalid paging parameters")
    })
    @GetMapping("/raw/{analysisId}")
    public Mono<ResponseEntity<Flux<RawDataResponse>>> getRawSensorData(
            @PathVariable @Min(1) Integer analysisId,
            @ParameterObject @Validated RawDataRequest request
    ) {
        return findHeader(analysisId)
                .map(header -> {
                    log.debug("Streaming raw data for analysisId={}, sensorType={}, page={}, size={}",
                            analysisId, request.getSensorType(), request.getPage(), request.getSize());
                    Flux<RawDataResponse> stream = dataService
                            .getRawData(analysisId,
                                    request.getSensorType(),
                                    request.getPage(),
                                    request.getSize())
                            .subscribeOn(Schedulers.boundedElastic());
                    return ResponseEntity.ok(stream);
                })
                .onErrorResume(ResourceNotFoundException.class, ex -> {
                    log.warn("Raw data request failed: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @Operation(
            summary     = "Get Detailed Sensor Measurements",
            description = "Retrieve paginated detailed sensor measurements for a given analysis ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detailed sensor measurements retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis ID not found"),
            @ApiResponse(responseCode = "400", description = "Invalid paging parameters")
    })
    @GetMapping("/detailed/{analysisId}")
    public Mono<ResponseEntity<Flux<RawDataResponse>>> getDetailedSensorData(
            @PathVariable @Min(1) Integer analysisId,
            @ParameterObject @Validated RawDataRequest request
    ) {
        return findHeader(analysisId)
                .map(header -> {
                    log.debug("Streaming detailed data for analysisId={}, page={}, size={}",
                            analysisId, request.getPage(), request.getSize());
                    Flux<RawDataResponse> stream = dataService
                            .getDetailedSensorData(analysisId,
                                    request.getPage(),
                                    request.getSize())
                            .subscribeOn(Schedulers.boundedElastic());
                    return ResponseEntity.ok(stream);
                })
                .onErrorResume(ResourceNotFoundException.class, ex -> {
                    log.warn("Detailed data request failed: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @Operation(
            summary     = "Get TP-wise Sensor Series",
            description = "Retrieve the series of TP labels and corresponding values for one sensor, over a time range and (optionally) a single analysis ID.",
            parameters  = {
                    @Parameter(
                            name        = "analysisId",
                            in          = ParameterIn.QUERY,
                            description = "Analysis ID (train/run identifier); optional",
                            required    = false,
                            schema      = @Schema(type = "integer", minimum = "1")
                    ),
                    @Parameter(
                            name        = "station",
                            in          = ParameterIn.QUERY,
                            description = "Station code: MP1, MP3, or BOTH",
                            required    = true,
                            schema      = @Schema(type = "string", allowableValues = {"MP1","MP3","BOTH"})
                    ),
                    @Parameter(
                            name        = "start",
                            in          = ParameterIn.QUERY,
                            description = "ISO-8601 start timestamp",
                            required    = true,
                            schema      = @Schema(type = "string", format = "date-time")
                    ),
                    @Parameter(
                            name        = "end",
                            in          = ParameterIn.QUERY,
                            description = "ISO-8601 end timestamp",
                            required    = true,
                            schema      = @Schema(type = "string", format = "date-time")
                    ),
                    @Parameter(
                            name        = "sensor",
                            in          = ParameterIn.QUERY,
                            description = "Sensor code; one of [speed, aoa, vfrcl, vfrcr, vvibl, vvibr, lfrcl, lfrcr, lvibl, lvibr, dtl, dtr, lngl, lngr]",
                            required    = true,
                            schema      = @Schema(type = "string")
                    )
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "TP series retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Analysis ID not found")
    })

    @GetMapping("/tp-series")
    public Mono<ResponseEntity<SensorTpSeriesDTO>> getTpSeries(
            @RequestParam(value = "analysisId", required = false)
            @Min(value = 1, message = "analysisId must be at least 1")
            Integer analysisId,

            @RequestParam("station") String station,

            @RequestParam("start")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,

            @RequestParam("end")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end,

            @RequestParam("sensor") String sensor
    ) {
        log.debug("tpSeries → analysisId={} station={} start={} end={} sensor={}",
                analysisId, station, start, end, sensor);

        Mono<Void> validation = analysisId == null ? Mono.empty() : findHeader(analysisId).then();

        return validation
                .then(dataService.tpSeries(analysisId, station, start, end, sensor))
                .map(ResponseEntity::ok)
                .onErrorResume(ResourceNotFoundException.class,
                        ex -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(IllegalArgumentException.class,
                        ex -> Mono.just(ResponseEntity.badRequest().build()));
    }

}
