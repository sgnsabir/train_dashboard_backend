package com.banenor.controller;

import com.banenor.dto.AxlesDataDTO;
import com.banenor.service.AxlesDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/axles")
@RequiredArgsConstructor
public class AxlesDataController {

    private final AxlesDataService axlesDataService;

    /**
     * GET /api/v1/axles/data
     */
    @GetMapping("/data")
    public Flux<AxlesDataDTO> getAxlesData(
            @RequestParam Integer trainNo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String measurementPoint
    ) {
        log.info("Fetching axles data for trainNo={}, start={}, end={}, measurementPoint={}",
                trainNo, start, end, measurementPoint);
        return axlesDataService.getAxlesData(trainNo, start, end, measurementPoint);
    }

    /**
     * GET /api/v1/axles/global-aggregations
     */
    @GetMapping("/global-aggregations")
    public Mono<AxlesDataDTO> getGlobalAggregations(
            @RequestParam String measurementPoint
    ) {
        log.info("Fetching global aggregations for measurementPoint={}", measurementPoint);
        return axlesDataService.getGlobalAggregations(measurementPoint);
    }
}
