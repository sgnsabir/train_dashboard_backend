package com.banenor.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.banenor.dto.AxlesDataDTO;
import com.banenor.service.AxlesDataService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/axles")
@RequiredArgsConstructor
public class AxlesDataController {

    private final AxlesDataService axlesDataService;

    @GetMapping("/data")
    public Flux<AxlesDataDTO> getAxlesData(
            @RequestParam Integer trainNo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String measurementPoint) {
        return axlesDataService.getAxlesData(trainNo, start, end, measurementPoint);
    }

    @GetMapping("/global-aggregations")
    public Mono<AxlesDataDTO> getGlobalAggregations(@RequestParam String measurementPoint) {
        return axlesDataService.getGlobalAggregations(measurementPoint);
    }
} 
