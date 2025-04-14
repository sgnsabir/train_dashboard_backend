package com.banenor.controller;

import com.banenor.dto.ReliabilityHealthDTO;
import com.banenor.service.ReliabilityHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping(value = "/api/v1/reliability-health", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
public class ReliabilityHealthController {

    private final ReliabilityHealthService reliabilityHealthService;

    /**
     * Retrieves the overall reliability health score for a given train.
     * This endpoint calculates a health score based on the latest sensor metrics.
     *
     * Example endpoint:
     * GET /api/v1/reliability-health?trainNo=123
     *
     * @param trainNo the train number identifier.
     * @return a Mono emitting a ResponseEntity containing ReliabilityHealthDTO.
     */
    @GetMapping
    public Mono<ResponseEntity<ReliabilityHealthDTO>> getReliabilityHealth(
            @RequestParam("trainNo") Integer trainNo) {

        log.info("Fetching reliability health data for trainNo={}", trainNo);

        return reliabilityHealthService.calculateHealthScore(trainNo)
                .map(ResponseEntity::ok)
                .doOnError(ex -> log.error("Error fetching reliability health data for trainNo {}: {}", trainNo, ex.getMessage(), ex))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
