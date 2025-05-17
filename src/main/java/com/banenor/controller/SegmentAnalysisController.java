package com.banenor.controller;

import com.banenor.dto.SegmentAnalysisDTO;
import com.banenor.dto.SegmentAnalysisRequest;
import com.banenor.service.SegmentAnalysisService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
     */
    @GetMapping
    public Flux<SegmentAnalysisDTO> getSegmentAnalysisData(
            @Validated SegmentAnalysisRequest req
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resolvedStart = (req.getStart() != null) ? req.getStart() : now.minusDays(7);
        LocalDateTime resolvedEnd   = (req.getEnd()   != null) ? req.getEnd()   : now;

        if (resolvedEnd.isBefore(resolvedStart)) {
            log.warn("Invalid time window: end {} is before start {}", resolvedEnd, resolvedStart);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request parameter 'end' must not be before 'start'"
            );
        }

        log.info("SegmentAnalysisController.getSegmentAnalysisData(trainNo={}, start={}, end={})",
                req.getTrainNo(), resolvedStart, resolvedEnd);

        return segmentAnalysisService
                .analyzeSegmentData(req.getTrainNo(), resolvedStart, resolvedEnd)
                .doOnError(ex -> log.error(
                        "Error fetching segment analysis for train {}: {}",
                        req.getTrainNo(), ex.getMessage(), ex))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
