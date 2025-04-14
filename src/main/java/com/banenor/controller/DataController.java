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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/data")
@Tag(name = "Data", description = "Endpoint for raw and detailed sensor data retrieval")
public class DataController {

    private static final Logger logger = LoggerFactory.getLogger(DataController.class);

    private final DataService dataService;
    private final HaugfjellMP1HeaderRepository mp1HeaderRepo;
    private final HaugfjellMP3HeaderRepository mp3HeaderRepo;

    public DataController(DataService dataService,
                          HaugfjellMP1HeaderRepository mp1HeaderRepo,
                          HaugfjellMP3HeaderRepository mp3HeaderRepo) {
        this.dataService = dataService;
        this.mp1HeaderRepo = mp1HeaderRepo;
        this.mp3HeaderRepo = mp3HeaderRepo;
    }

    /**
     * Finds the header corresponding to the given analysis ID by checking both MP1 and MP3 repositories.
     * If not found, logs the error and returns a ResourceNotFoundException.
     */
    private Mono<AbstractHeader> findHeader(Integer analysisId) {
        return Mono.firstWithValue(
                        mp1HeaderRepo.findById(analysisId).cast(AbstractHeader.class),
                        mp3HeaderRepo.findById(analysisId).cast(AbstractHeader.class)
                )
                .switchIfEmpty(Mono.defer(() -> {
                    logger.error("Header not found for analysis ID: {}", analysisId);
                    return Mono.error(new ResourceNotFoundException("Analysis ID " + analysisId + " not found."));
                }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Retrieves paginated raw sensor data for the specified analysis ID, with optional sensor type filtering.
     */
    @Operation(summary = "Get Raw Sensor Data", description = "Retrieve paginated raw sensor data for a given analysis ID with optional sensor type filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Raw sensor data retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis ID not found")
    })
    @GetMapping("/raw/{analysisId}")
    public Flux<RawDataResponse> getRawSensorData(
            @PathVariable Integer analysisId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sensorType) {
        return findHeader(analysisId)
                .thenMany(dataService.getRawData(analysisId, sensorType, page, size))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> logger.error("Error retrieving raw sensor data for analysis ID {}: {}", analysisId, ex.getMessage()));
    }

    /**
     * Retrieves paginated detailed sensor measurements for the specified analysis ID.
     */
    @Operation(summary = "Get Detailed Sensor Measurements", description = "Retrieve paginated detailed sensor measurements for a given analysis ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detailed sensor measurements retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Analysis ID not found")
    })
    @GetMapping("/detailed/{analysisId}")
    public Flux<SensorMeasurementDTO> getDetailedSensorData(
            @PathVariable Integer analysisId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return findHeader(analysisId)
                .thenMany(dataService.getDetailedSensorData(analysisId, page, size))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> logger.error("Error retrieving detailed sensor data for analysis ID {}: {}", analysisId, ex.getMessage()));
    }
}
