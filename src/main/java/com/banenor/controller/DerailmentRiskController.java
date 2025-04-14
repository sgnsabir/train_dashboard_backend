package com.banenor.controller;

import com.banenor.dto.DerailmentRiskDTO;
import com.banenor.service.DerailmentRiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping(value = "/api/v1/derailment", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
public class DerailmentRiskController {

    private final DerailmentRiskService derailmentRiskService;

    /**
     * Retrieves derailment risk analysis data for the specified train and time range.
     *
     * Example endpoint:
     * GET /api/v1/derailment?trainNo=123&startDate=2025-01-01T00:00:00&endDate=2025-01-08T00:00:00
     *
     * @param trainNo   the train number identifier
     * @param startDate optional start date in ISO-8601 format (defaults to now minus 7 days)
     * @param endDate   optional end date in ISO-8601 format (defaults to now)
     * @return a Flux stream of DerailmentRiskDTO objects representing analyzed sensor data
     */
    @GetMapping
    public Flux<DerailmentRiskDTO> getDerailmentRiskData(
            @RequestParam("trainNo") Integer trainNo,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String endDate) {

        LocalDateTime start = parseOrDefault(startDate, LocalDateTime.now().minusDays(7));
        LocalDateTime end = parseOrDefault(endDate, LocalDateTime.now());

        log.info("Fetching derailment risk data for trainNo={} from {} to {}", trainNo, start, end);

        return derailmentRiskService.fetchDerailmentRiskData(trainNo, start, end)
                .doOnError(ex -> log.error("Error fetching derailment risk data for trainNo {}: {}", trainNo, ex.getMessage(), ex))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Helper method to parse an ISO-8601 date/time string.
     * Returns the default value if parsing fails or the input is null/empty.
     *
     * @param dateTimeStr  the date/time string to parse
     * @param defaultValue the default LocalDateTime to use if parsing fails
     * @return the parsed LocalDateTime or the default value
     */
    private LocalDateTime parseOrDefault(String dateTimeStr, LocalDateTime defaultValue) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return LocalDateTime.parse(dateTimeStr);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date/time '{}', falling back to default value {}", dateTimeStr, defaultValue);
            return defaultValue;
        }
    }
}
