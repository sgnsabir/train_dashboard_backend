package com.banenor.controller;

import com.banenor.dto.WheelConditionDTO;
import com.banenor.service.WheelConditionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping(value = "/api/v1/wheel", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Wheel Condition", description = "Endpoints for wheel condition analysis")
@PreAuthorize("hasRole('MAINTENANCE')")
public class WheelConditionController {

    private final WheelConditionService wheelConditionService;

    /**
     * Fetch wheel condition data for a train within a date range.
     * Defaults to the last 7 days if dates are omitted or invalid.
     */
    @GetMapping
    public Flux<WheelConditionDTO> getWheelConditionData(
            @RequestParam("trainNo") @Min(1) Integer trainNo,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate",   required = false) String endDate) {

        LocalDateTime start = parseOrDefault(startDate, LocalDateTime.now().minusDays(7));
        LocalDateTime end   = parseOrDefault(endDate,   LocalDateTime.now());

        log.info("Fetching wheel condition for trainNo={} from {} to {}", trainNo, start, end);

        return wheelConditionService
                .fetchWheelConditionData(trainNo, start, end)
                .doOnError(ex -> log.error("Error fetching wheel condition for trainNo {}: {}", trainNo, ex.getMessage(), ex))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Parse an ISO‑8601 date-time or date string, falling back to default on parse failure.
     */
    private LocalDateTime parseOrDefault(String dateTimeStr, LocalDateTime defaultValue) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return defaultValue;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                LocalDate date = LocalDate.parse(dateTimeStr, DateTimeFormatter.ISO_DATE);
                return date.atStartOfDay();
            } catch (DateTimeParseException ex) {
                log.warn("Invalid date '{}', using default {}", dateTimeStr, defaultValue);
                return defaultValue;
            }
        }
    }
}
