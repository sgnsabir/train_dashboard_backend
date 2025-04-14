// src/main/java/com/banenor/controller/WheelConditionController.java

package com.banenor.controller;

import com.banenor.dto.WheelConditionDTO;
import com.banenor.service.WheelConditionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
public class WheelConditionController {

    private final WheelConditionService wheelConditionService;

    /**
     * Retrieves wheel condition analysis data for a given train within the specified date range.
     * If startDate and/or endDate are not provided, defaults to the last 7 days.
     * Example endpoint:
     * GET /api/v1/wheel?trainNo=123&startDate=2025-01-01T00:00:00&endDate=2025-01-08T00:00:00
     *
     * @param trainNo   the train number identifier.
     * @param startDate optional ISO-8601 formatted start date/time or date string.
     * @param endDate   optional ISO-8601 formatted end date/time or date string.
     * @return a Flux of WheelConditionDTO objects representing the wheel condition analysis.
     */
    @GetMapping
    public Flux<WheelConditionDTO> getWheelConditionData(
            @RequestParam("trainNo") Integer trainNo,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        // If date parameters are missing or not in the expected format,
        // default to last 7 days for start and now for end.
        LocalDateTime start = parseOrDefault(startDate, LocalDateTime.now().minusDays(7));
        LocalDateTime end = parseOrDefault(endDate, LocalDateTime.now());

        log.info("Fetching wheel condition data for trainNo={} from {} to {}", trainNo, start, end);

        return wheelConditionService.fetchWheelConditionData(trainNo, start, end)
                .doOnError(ex -> log.error("Error fetching wheel condition data for trainNo {}: {}", trainNo, ex.getMessage(), ex))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Robustly parses a date/time string.
     * The method first attempts to parse the input string as a full ISO‑8601 date/time.
     * If that fails, it attempts to parse it as an ISO‑8601 date (without time) and returns the start of the day.
     * If parsing fails altogether, logs a warning and returns the provided default value.
     *
     * @param dateTimeStr  the date/time string to parse.
     * @param defaultValue the default LocalDateTime value to return on failure.
     * @return the parsed LocalDateTime or the default value.
     */
    private LocalDateTime parseOrDefault(String dateTimeStr, LocalDateTime defaultValue) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            // First try parsing as a date-time with time component (e.g., 2025-01-01T00:00:00)
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                // If that fails, attempt to parse as a simple date (e.g., 2025-01-01) and return the start of the day
                LocalDate date = LocalDate.parse(dateTimeStr, DateTimeFormatter.ISO_DATE);
                return date.atStartOfDay();
            } catch (DateTimeParseException ex) {
                log.warn("Failed to parse date/time '{}'. Falling back to default value: {}", dateTimeStr, defaultValue);
                return defaultValue;
            }
        }
    }
}
