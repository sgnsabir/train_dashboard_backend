package com.banenor.controller;

import com.banenor.dto.TrackConditionDTO;
import com.banenor.service.TrackConditionService;
import com.banenor.util.DateTimeUtils;
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
import java.time.LocalDateTime;

@RestController
@RequestMapping(value = "/api/v1/track", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Track Condition", description = "Endpoints for track condition analysis")
@Validated
@PreAuthorize("hasRole('MAINTENANCE')")
public class TrackConditionController {

    private final TrackConditionService trackConditionService;

    @Operation(
            summary = "Get Track Condition",
            description = "Retrieve lateral and vertical force metrics for a given train over a specified time range."
    )
    @GetMapping
    public Flux<TrackConditionDTO> getTrackConditionData(
            @RequestParam("trainNo") @Min(1) Integer trainNo,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String endDate) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = DateTimeUtils.parseOrDefault(startDate, now.minusDays(7));
        LocalDateTime to   = DateTimeUtils.parseOrDefault(endDate, now);

        log.info("Fetching track condition for trainNo={} from {} to {}", trainNo, from, to);

        return trackConditionService
                .fetchTrackConditionData(trainNo, from, to)
                .doOnError(ex -> log.error("Error fetching track condition for trainNo={}: {}", trainNo, ex.getMessage(), ex))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
