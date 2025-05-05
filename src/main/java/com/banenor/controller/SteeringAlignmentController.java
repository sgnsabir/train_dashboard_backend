package com.banenor.controller;

import com.banenor.dto.SteeringAlignmentDTO;
import com.banenor.service.SteeringAlignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@RestController
@RequestMapping(value = "/api/v1/steering", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Steering Alignment", description = "Endpoints for steering alignment analysis")
@PreAuthorize("hasRole('MAINTENANCE')")
@Validated
@RequiredArgsConstructor
@Slf4j
public class SteeringAlignmentController {

    private final SteeringAlignmentService steeringService;

    @Operation(
            summary = "Get Steering Alignment Data",
            description = "Retrieve steering alignment analysis for a train over a date range"
    )
    @GetMapping
    public Flux<SteeringAlignmentDTO> getSteeringData(
            @RequestParam("trainNo") @Min(value = 1, message = "trainNo must be >= 1") Integer trainNo,
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(value = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = (start != null ? start : now.minusDays(7));
        LocalDateTime to   = (end   != null ? end   : now);

        log.info("Fetching steering alignment for trainNo={} from {} to {}", trainNo, from, to);

        return steeringService.fetchSteeringData(trainNo, from, to)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> log.error(
                        "Error fetching steering alignment for trainNo {}: {}", trainNo, ex.getMessage(), ex
                ));
    }
}
