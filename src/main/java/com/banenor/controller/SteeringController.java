package com.banenor.controller;

import com.banenor.dto.SteeringAlignmentDTO;
import com.banenor.service.SteeringAlignmentService;
import com.banenor.util.DateTimeUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import java.time.LocalDateTime;

/**
 * Controller for steering alignment analysis.
 * Provides endpoints to fetch steering analysis data for a specified train within a given date range.
 */
@RestController
@RequestMapping(value = "/api/v1/steering", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Steering", description = "Endpoints for steering alignment analysis")
public class SteeringController {

    private final SteeringAlignmentService steeringService;

    /**
     * Retrieves steering alignment data for a specified train and date range.
     * If startDate and/or endDate are not provided or invalid, defaults to the last 7 days.
     *
     * @param trainNo   the train number (required).
     * @param startDate optional ISO-8601 formatted start date/time.
     * @param endDate   optional ISO-8601 formatted end date/time.
     * @return a Flux of SteeringAlignmentDTO objects.
     */
    @GetMapping
    public Flux<SteeringAlignmentDTO> getSteeringData(
            @RequestParam @NotNull Integer trainNo,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime defaultStart = now.minusDays(7);
        LocalDateTime defaultEnd = now;

        LocalDateTime start = DateTimeUtils.parseOrDefault(startDate, defaultStart);
        LocalDateTime end = DateTimeUtils.parseOrDefault(endDate, defaultEnd);

        log.info("Fetching steering data for trainNo={} from {} to {}", trainNo, start, end);

        return steeringService.fetchSteeringData(trainNo, start, end)
                .doOnError(ex -> log.error("Error fetching steering data: {}", ex.getMessage(), ex))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
