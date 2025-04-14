// src/main/java/com/banenor/controller/SegmentAnalysisController.java

package com.banenor.controller;

import com.banenor.dto.SegmentAnalysisDTO;
import com.banenor.service.SegmentAnalysisService;
import com.banenor.util.DateTimeUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

/**
 * Controller for segment analysis.
 * Retrieves sensor data grouped by track segment and aggregates anomaly counts.
 */
@RestController
@RequestMapping(value = "/api/v1/segment-analysis", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Segment Analysis", description = "Endpoints for segment-based sensor data analysis")
public class SegmentAnalysisController {

    private final SegmentAnalysisService segmentAnalysisService;

    /**
     * Retrieves segment analysis data for the specified train number and date range.
     * If the startDate and/or endDate are not provided or invalid, defaults to the last 7 days.
     *
     * Example endpoint:
     * GET /api/v1/segment-analysis?trainNo=123&startDate=2025-01-01T00:00:00&endDate=2025-01-08T00:00:00
     *
     * @param trainNo   the train number identifier (must be greater than zero).
     * @param startDate optional ISO-8601 formatted start date/time.
     * @param endDate   optional ISO-8601 formatted end date/time.
     * @return a Flux of SegmentAnalysisDTO objects.
     */
    @GetMapping
    public Flux<SegmentAnalysisDTO> getSegmentAnalysisData(
            @RequestParam("trainNo") Integer trainNo,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        if (trainNo == null || trainNo <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid trainNo provided.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime defaultStart = now.minusDays(7);
        LocalDateTime defaultEnd = now;

        // Parse provided dates or fallback to defaults
        LocalDateTime start = DateTimeUtils.parseOrDefault(startDate, defaultStart);
        LocalDateTime end = DateTimeUtils.parseOrDefault(endDate, defaultEnd);

        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate cannot be earlier than startDate.");
        }

        log.info("Fetching segment analysis data for trainNo={} from {} to {}", trainNo, start, end);

        return segmentAnalysisService.analyzeSegmentData(trainNo, start, end)
                .subscribeOn(Schedulers.boundedElastic());
    }
}
