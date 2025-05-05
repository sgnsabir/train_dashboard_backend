package com.banenor.controller;

import com.banenor.dto.RawDataResponse;
import com.banenor.service.DataService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Controller for fetching raw sensor data with paging and optional type filtering.
 */
@RestController
@RequestMapping("/api/v1/raw")
@Validated
@Slf4j
@RequiredArgsConstructor
public class RawDataController {

    private final DataService dataService;

    /**
     * Fetch paginated raw data responses for a given analysis ID, optionally filtered by sensor type.
     *
     * @param analysisId the analysis identifier (must be >= 1)
     * @param sensorType optional sensor type to filter (e.g. "spd", "aoa")
     * @param page       zero-based page index (must be >= 0)
     * @param size       page size (must be >= 1)
     * @return a Flux of RawDataResponse DTOs
     */
    @GetMapping("/{analysisId}")
    public Flux<RawDataResponse> getRawData(
            @PathVariable @Min(value = 1, message = "analysisId must be >= 1") Integer analysisId,
            @RequestParam(required = false) String sensorType,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be >= 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be >= 1") int size
    ) {
        log.debug("GET /api/raw/{}?sensorType={}&page={}&size={} called", analysisId, sensorType, page, size);

        return dataService.getRawData(analysisId, sensorType, page, size)
                .doOnNext(dto -> log.trace("Emitting RawDataResponse: {}", dto))
                .doOnComplete(() -> log.debug("Completed fetching raw data for analysisId={}", analysisId))
                .doOnError(error -> log.error("Error fetching raw data for analysisId={}", analysisId, error));
    }
}
