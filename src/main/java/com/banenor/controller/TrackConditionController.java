package com.banenor.controller;

import com.banenor.dto.TrackConditionDTO;
import com.banenor.dto.TrackConditionRequest;
import com.banenor.service.TrackConditionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import jakarta.validation.Valid;
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
            @Valid @ParameterObject TrackConditionRequest req
    ) {
        // Determine window defaults
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = Optional.ofNullable(req.getStart()).orElse(now.minusDays(7));
        LocalDateTime to   = Optional.ofNullable(req.getEnd()).orElse(now);

        log.info("Fetching track condition [trainNo={}, from={}, to={}]",
                req.getTrainNo(), from, to);

        return trackConditionService
                .fetchTrackConditionData(req.getTrainNo(), from, to)
                .onBackpressureLatest()
                .sample(Duration.ofMillis(500))
                .doOnError(ex ->
                        log.error("Error streaming track condition for trainNo={}: {}",
                                req.getTrainNo(), ex.getMessage(), ex)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }
}
