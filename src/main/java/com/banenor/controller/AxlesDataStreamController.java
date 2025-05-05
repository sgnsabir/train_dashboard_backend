package com.banenor.controller;

import com.banenor.dto.AxlesDataDTO;
import com.banenor.service.KafkaSsePublisherService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/stream")
@CrossOrigin(origins = "${websocket.allowedOrigins}")
@RequiredArgsConstructor
@Validated
public class AxlesDataStreamController {

    private final KafkaSsePublisherService ssePublisher;

    /**
     * Streams live per-TP axle readings as Server-Sent Events from Kafka.
     *
     * @param trainNo          the train number (must be ≥1)
     * @param measurementPoint e.g. "TP1" (must be non-blank)
     * @param start            optional ISO-8601 start timestamp (defaults to now minus 1h)
     * @param end              optional ISO-8601 end timestamp (defaults to now)
     */
    @GetMapping(value = "/axles", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AxlesDataDTO> streamAxlesData(
            @RequestParam @Min(1) Integer trainNo,
            @RequestParam @NotBlank String measurementPoint,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end
    ) {
        // apply defaults if missing
        LocalDateTime startTime = Optional.ofNullable(start)
                .orElse(LocalDateTime.now().minusHours(1));
        LocalDateTime endTime = Optional.ofNullable(end)
                .orElse(LocalDateTime.now());

        log.info("Opened SSE subscription → train={}, tp={}, start={}, end={}",
                trainNo, measurementPoint, startTime, endTime);

        return ssePublisher.stream(trainNo, measurementPoint)
                .doOnNext(dto ->
                        log.debug("Emitting SSE payload from Kafka → {}", dto))
                .doOnError(err ->
                        log.error("Error in Kafka SSE stream for train={} tp={}", trainNo, measurementPoint, err))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5))
                        .maxBackoff(Duration.ofSeconds(60))
                        .filter(ex -> !(ex instanceof IllegalArgumentException))
                        .doBeforeRetry(sig ->
                                log.warn("Retrying SSE (train={}, tp={}) due to: {}",
                                        trainNo, measurementPoint, sig.failure().getMessage()))
                );
    }
}
