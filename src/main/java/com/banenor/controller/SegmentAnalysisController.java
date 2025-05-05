package com.banenor.controller;

import com.banenor.dto.SegmentAnalysisDTO;
import com.banenor.service.SegmentAnalysisService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@RestController
@RequestMapping(path = "/api/v1/segment-analysis", produces = "application/json")
@Tag(name = "Segment Analysis", description = "Endpoints for segment-based sensor data analysis")
@Validated
@RequiredArgsConstructor
@Slf4j
public class SegmentAnalysisController {

    private final SegmentAnalysisService segmentAnalysisService;

    /**
     * Fetches segment-based analysis for a train over a given time window.
     * Defaults to the last 7 days when dates are omitted.
     *
     * @param trainNo the train number (>0)
     * @param start   optional ISO-date-time; defaults to now().minusDays(7)
     * @param end     optional ISO-date-time; defaults to now()
     */
    @GetMapping
    public Flux<SegmentAnalysisDTO> getSegmentAnalysisData(
            @RequestParam("trainNo")
            @Min(value = 1, message = "trainNo must be greater than zero")
            Integer trainNo,

            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,

            @RequestParam(value = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resolvedStart = (start != null) ? start : now.minusDays(7);
        LocalDateTime resolvedEnd   = (end   != null) ? end   : now;

        if (resolvedEnd.isBefore(resolvedStart)) {
            log.warn("Invalid time window: end {} is before start {}", resolvedEnd, resolvedStart);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request parameter 'end' must not be before 'start'"
            );
        }

        log.info("SegmentAnalysisController.getSegmentAnalysisData(trainNo={}, start={}, end={})",
                trainNo, resolvedStart, resolvedEnd);

        return segmentAnalysisService
                .analyzeSegmentData(trainNo, resolvedStart, resolvedEnd)
                .doOnError(ex -> log.error(
                        "Error fetching segment analysis for train {}: {}",
                        trainNo, ex.getMessage(), ex))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
