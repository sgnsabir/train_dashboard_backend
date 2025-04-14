package com.banenor.controller;

import com.banenor.dto.PerformanceDTO;
import com.banenor.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@RestController
@RequestMapping(value = "/api/v1/performance", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    @GetMapping
    public Flux<PerformanceDTO> getPerformanceData(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveStart = (startDate != null) ? startDate : now.minusDays(7);
        LocalDateTime effectiveEnd = (endDate != null) ? endDate : now;

        log.info("Fetching performance data from {} to {}", effectiveStart, effectiveEnd);

        return performanceService.getPerformanceData(effectiveStart, effectiveEnd)
                .doOnError(ex -> log.error("Error fetching performance data: {}", ex.getMessage(), ex))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
