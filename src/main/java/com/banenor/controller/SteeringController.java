package com.banenor.controller;

import com.banenor.dto.SteeringAlignmentDTO;
import com.banenor.service.SteeringAlignmentService;
import com.banenor.util.DateTimeUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@RestController
@RequestMapping(value = "/api/v1/steering", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Steering Alignment", description = "Endpoints for steering alignment analysis")
@PreAuthorize("hasRole('MAINTENANCE')")
@RequiredArgsConstructor
@Slf4j
public class SteeringController {

    private final SteeringAlignmentService steeringService;

    @Operation(
            summary = "Get Steering Alignment Data",
            description = "Retrieve steering alignment analysis for a train over a date range"
    )
    @GetMapping
    public Flux<SteeringAlignmentDTO> getSteeringData(
            @RequestParam("trainNo") @Min(1) Integer trainNo,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = DateTimeUtils.parseOrDefault(startDate, now.minusDays(7));
        LocalDateTime end = DateTimeUtils.parseOrDefault(endDate, now);

        log.info("Fetching steering alignment for trainNo={} from {} to {}", trainNo, start, end);

        return steeringService.fetchSteeringData(trainNo, start, end)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> log.error("Error fetching steering alignment for trainNo {}: {}", trainNo, ex.getMessage(), ex));
    }
}
