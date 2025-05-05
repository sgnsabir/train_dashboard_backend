package com.banenor.controller;

import com.banenor.dto.TrackConditionDTO;
import com.banenor.service.TrackConditionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/track")
@Slf4j
@Validated
@RequiredArgsConstructor
@Tag(name = "Track Condition", description = "Endpoints for track condition analysis")
@PreAuthorize("hasRole('MAINTENANCE')")
public class TrackConditionController {

    private final TrackConditionService trackConditionService;

    @Operation(
            summary = "Stream Track Condition Metrics",
            description = "Stream lateral and vertical force metrics for a given train over a time window (SSE or ND-JSON)."
    )
    @GetMapping(produces = {
            MediaType.TEXT_EVENT_STREAM_VALUE,
            "application/x-ndjson"
    })
    public Flux<TrackConditionDTO> getTrackConditionData(
            @RequestParam("trainNo")
            @Min(value = 1, message = "trainNo must be ≥ 1")
            Integer trainNo,

            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,

            @RequestParam(value = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end
    ) {
        // Apply defaults if parameters are missing
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = Optional.ofNullable(start).orElse(now.minusDays(7));
        LocalDateTime to   = Optional.ofNullable(end).orElse(now);

        log.info("Fetching track condition [trainNo={}, from={}, to={}]", trainNo, from, to);

        return trackConditionService
                .fetchTrackConditionData(trainNo, from, to)
                .onBackpressureLatest()
                .sample(Duration.ofMillis(500))
                .doOnError(ex ->
                        log.error("Error streaming track condition for trainNo={}: {}", trainNo, ex.getMessage(), ex)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }
}
