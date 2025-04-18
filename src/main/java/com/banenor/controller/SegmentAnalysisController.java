package com.banenor.controller;

import com.banenor.dto.SegmentAnalysisDTO;
import com.banenor.service.SegmentAnalysisService;
import com.banenor.util.DateTimeUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@RestController
@RequestMapping(value = "/api/v1/segment-analysis", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Segment Analysis", description = "Endpoints for segment-based sensor data analysis")
@Validated
@PreAuthorize("hasRole('MAINTENANCE')")
@RequiredArgsConstructor
@Slf4j
public class SegmentAnalysisController {

    private final SegmentAnalysisService segmentAnalysisService;

    /**
     * Fetches segment-based analysis for a train over a given time window.
     * Defaults to the last 7 days when dates are omitted or invalid.
     */
    @GetMapping
    public Flux<SegmentAnalysisDTO> getSegmentAnalysisData(
            @RequestParam("trainNo") @Min(value = 1, message = "trainNo must be greater than zero") Integer trainNo,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String endDate) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = DateTimeUtils.parseOrDefault(startDate, now.minusDays(7));
        LocalDateTime end =   DateTimeUtils.parseOrDefault(endDate,   now);

        if (end.isBefore(start)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "endDate must not be before startDate"
            );
        }

        log.info("Segment analysis request: trainNo={}, start={}, end={}", trainNo, start, end);
        return segmentAnalysisService
                .analyzeSegmentData(trainNo, start, end)
                .doOnError(ex -> log.error(
                        "Failed to fetch segment analysis for train {}: {}",
                        trainNo, ex.getMessage(), ex))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
