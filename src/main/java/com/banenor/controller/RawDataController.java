package com.banenor.controller;

import com.banenor.dto.RawDataPageRequest;
import com.banenor.dto.RawDataResponse;
import com.banenor.service.DataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import jakarta.validation.constraints.Min;

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
     * Fetch paginated raw data for a given analysis ID, optionally filtering by sensor type.
     *
     * @param analysisId the analysis identifier (must be ≥ 1)
     * @param pageReq    page & filter parameters
     * @return flux of RawDataResponse
     */
    @GetMapping("/{analysisId}")
    public Flux<RawDataResponse> getRawData(
            @PathVariable @Min(value = 1, message = "analysisId must be >= 1")
            Integer analysisId,
            @ParameterObject @Validated
            RawDataPageRequest pageReq
    ) {
        log.debug("GET /api/v1/raw/{}?sensorType={}&page={}&size={}",
                analysisId,
                pageReq.getSensorType(),
                pageReq.getPage(),
                pageReq.getSize());

        return dataService.getRawData(
                        analysisId,
                        pageReq.getSensorType(),
                        pageReq.getPage(),
                        pageReq.getSize()
                )
                .doOnNext(dto -> log.trace("Emitting RawDataResponse: {}", dto))
                .doOnComplete(()  -> log.debug("Completed fetching raw data for analysisId={}", analysisId))
                .doOnError(error -> log.error("Error fetching raw data for analysisId={}", analysisId, error));
    }
}
