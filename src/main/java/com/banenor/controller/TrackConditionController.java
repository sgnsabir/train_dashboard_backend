package com.banenor.controller;

import com.banenor.dto.TrackConditionDTO;
import com.banenor.service.TrackConditionService;
import com.banenor.util.DateTimeUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

/**
 * Controller for track condition analysis.
 * Retrieves track condition data for a given train number and date range,
 * and analyzes lateral and vertical forces.
 */
@RestController
@RequestMapping(value = "/api/v1/track", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Track Condition", description = "Endpoints for track condition analysis")
public class TrackConditionController {

    private final TrackConditionService trackConditionService;

    /**
     * Retrieves track condition data for the specified train number within a given date range.
     * If the startDate and/or endDate parameters are missing or invalid, defaults to the last 7 days.
     * Example endpoint:
     * GET /api/v1/track?trainNo=123&startDate=2025-01-01T00:00:00&endDate=2025-01-08T00:00:00
     *
     * @param trainNo   the train number.
     * @param startDate optional ISO-8601 formatted start date/time.
     * @param endDate   optional ISO-8601 formatted end date/time.
     * @return a Flux of TrackConditionDTO objects.
     */
    @GetMapping
    public Flux<TrackConditionDTO> getTrackConditionData(
            @RequestParam("trainNo") Integer trainNo,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime defaultStart = now.minusDays(7);
        LocalDateTime defaultEnd = now;

        LocalDateTime start = DateTimeUtils.parseOrDefault(startDate, defaultStart);
        LocalDateTime end = DateTimeUtils.parseOrDefault(endDate, defaultEnd);

        log.info("Fetching track condition data for trainNo={} from {} to {}", trainNo, start, end);

        return trackConditionService.fetchTrackConditionData(trainNo, start, end)
                .doOnError(ex -> log.error("Error fetching track condition data: {}", ex.getMessage(), ex))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
