package com.banenor.controller;

import com.banenor.dto.WheelConditionDTO;
import com.banenor.dto.WheelConditionRequest;
import com.banenor.service.WheelConditionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Revised controller to use a single request DTO for wheel condition parameters.
 */
@RestController
@RequestMapping(value = "/api/v1/wheel", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Wheel Condition", description = "Endpoints for wheel condition analysis")
@PreAuthorize("hasRole('MAINTENANCE')")
public class WheelConditionController {

    private final WheelConditionService wheelConditionService;

    /**
     * Fetch wheel condition data for a train within a date range.
     * Defaults to the last 7 days if parameters are omitted.
     */
    @GetMapping
    public Flux<WheelConditionDTO> getWheelConditionData(
            @Valid WheelConditionRequest req
    ) {
        Integer trainNo = req.getTrainNo();
        LocalDateTime from = Optional.ofNullable(req.getStart())
                .orElse(LocalDateTime.now().minusDays(7));
        LocalDateTime to = Optional.ofNullable(req.getEnd())
                .orElse(LocalDateTime.now());

        log.info("Fetching wheel condition for trainNo={} from {} to {}", trainNo, from, to);

        return wheelConditionService
                .fetchWheelConditionData(trainNo, from, to)
                .doOnError(ex ->
                        log.error("Error fetching wheel condition [trainNo={}]: {}", trainNo, ex.getMessage(), ex)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }
}
