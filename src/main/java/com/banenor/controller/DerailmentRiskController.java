package com.banenor.controller;

import com.banenor.dto.DerailmentRiskDTO;
import com.banenor.service.DerailmentRiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/derailment", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Derailment Risk", description = "Analyze derailment risk over time ranges")
@PreAuthorize("hasRole('MAINTENANCE')")
@RequiredArgsConstructor
public class DerailmentRiskController {

    private final DerailmentRiskService derailmentRiskService;

    @Operation(
            summary = "Get Derailment Risk",
            description = "Retrieve derailment risk data for a given train and time range"
    )
    @GetMapping
    public Flux<DerailmentRiskDTO> getDerailmentRiskData(
            @RequestParam("trainNo") @Min(1) Integer trainNo,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();
        LocalDateTime start = startDate != null ? startDate : end.minusDays(7);

        if (start.isAfter(end)) {
            log.warn("startDate {} is after endDate {}", startDate, endDate);
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "startDate must be before endDate"
            );
        }

        log.info("Fetching derailment risk for trainNo={} from {} to {}", trainNo, start, end);

        return derailmentRiskService.fetchDerailmentRiskData(trainNo, start, end)
                .doOnError(ex -> log.error(
                        "Error fetching derailment risk for trainNo {}: {}", trainNo, ex.getMessage(), ex
                ))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
