package com.banenor.controller;

import com.banenor.dto.TrainHealthDTO;
import com.banenor.service.TrainHealthService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/health")
@Validated
@Slf4j
@RequiredArgsConstructor
public class TrainHealthController {

    private final TrainHealthService trainHealthService;

    /**
     * Stream all health records for a given train, newest first.
     *
     * @param trainNo the train number (must be ≥ 1)
     * @return NDJSON stream of TrainHealthDTO
     */
    @GetMapping(value = "/{trainNo}", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<TrainHealthDTO> getHealthForTrain(
            @PathVariable @Min(value = 1, message = "trainNo must be greater than or equal to 1") Integer trainNo
    ) {
        log.info("Streaming health records for train {}", trainNo);
        return trainHealthService.getHealthForTrain(trainNo)
                .doOnError(ex -> log.error("Error streaming health for train {}: {}", trainNo, ex.getMessage(), ex));
    }
}
