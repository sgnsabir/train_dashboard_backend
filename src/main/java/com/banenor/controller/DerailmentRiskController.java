package com.banenor.controller;

import com.banenor.dto.DerailmentRiskDTO;
import com.banenor.dto.DerailmentRiskRequest;
import com.banenor.service.DerailmentRiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Validated
@RestController
@RequestMapping(
        value = "/api/v1/derailment",
        produces = MediaType.APPLICATION_JSON_VALUE
)
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
            @Valid @ParameterObject DerailmentRiskRequest req
    ) {
        Integer trainNo = req.getTrainNo();
        LocalDateTime actualEnd = Optional.ofNullable(req.getEnd()).orElse(LocalDateTime.now());
        LocalDateTime actualStart = Optional.ofNullable(req.getStart()).orElse(actualEnd.minusDays(7));

        if (actualStart.isAfter(actualEnd)) {
            log.warn("Invalid date range: start={} is after end={}", actualStart, actualEnd);
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "start must be before or equal to end"
            );
        }

        log.info("Fetching derailment risk for trainNo={} from {} to {}", trainNo, actualStart, actualEnd);

        return derailmentRiskService
                .fetchDerailmentRiskData(trainNo, actualStart, actualEnd)
                .doOnError(ex -> log.error(
                        "Error fetching derailment risk for trainNo {}: {}",
                        trainNo, ex.getMessage(), ex
                ))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
