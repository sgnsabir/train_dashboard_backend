package com.banenor.controller;

import com.banenor.dto.SensorMetricsDTO;
import com.banenor.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@RestController
@RequestMapping(value = "/api/v1/realtime/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@Slf4j
@RequiredArgsConstructor
public class RealtimeSubscriptionController {

    private final DashboardService dashboardService;

    /**
     * Streams the latest sensor metrics as Server-Sent Events (SSE) for the specified train.
     * This endpoint polls the latest metrics every 5 seconds.
     *
     * Example:
     * GET /api/v1/realtime/stream?trainNo=123
     *
     * @param trainNo the train number (or analysis ID) for which to fetch realtime metrics.
     * @return a Flux of ServerSentEvent wrapping SensorMetricsDTO objects.
     */
    @GetMapping
    public Flux<ServerSentEvent<SensorMetricsDTO>> streamRealtimeMetrics(@RequestParam("trainNo") Integer trainNo) {
        log.info("Starting realtime SSE stream for trainNo={}", trainNo);

        return Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick -> dashboardService.getLatestMetrics(trainNo)
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume(ex -> {
                            log.error("Error fetching realtime metrics for trainNo {}: {}", trainNo, ex.getMessage(), ex);
                            return Mono.empty();
                        }))
                .map(metrics -> ServerSentEvent.<SensorMetricsDTO>builder()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .event("sensor-metrics")
                        .data(metrics)
                        .build())
                .doOnCancel(() -> log.info("Realtime SSE stream cancelled for trainNo={}", trainNo));
    }
}
