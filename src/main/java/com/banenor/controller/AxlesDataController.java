package com.banenor.controller;

import com.banenor.dto.AxlesDataDTO;
import com.banenor.dto.AxlesDataRequest;
import com.banenor.exception.ApiError;
import com.banenor.model.HaugfjellMP1Header;
import com.banenor.model.HaugfjellMP3Header;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.repository.HaugfjellMP3HeaderRepository;
import com.banenor.service.AxlesDataService;
import com.banenor.service.KafkaSsePublisherService;
import com.banenor.service.SensorDataAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/axles")
@CrossOrigin(origins = "${websocket.allowedOrigins}")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Axles Data",
        description = "Time-series & real-time per-train or per-station axle readings, plus sensor aggregation triggers"
)
public class AxlesDataController {

    private static final List<String> VALID_STATIONS = List.of("MP1", "MP3");
    private static final Duration SSE_RETRY_BACKOFF = Duration.ofSeconds(5);

    private final HaugfjellMP1HeaderRepository mp1HeaderRepo;
    private final HaugfjellMP3HeaderRepository mp3HeaderRepo;
    private final AxlesDataService axlesDataService;
    private final KafkaSsePublisherService ssePublisher;
    private final SensorDataAggregationService sensorDataAggregationService;

    //───────────────────────────────────────────────────────────────────────────────
    // 1) Per-train historical & aggregate
    //───────────────────────────────────────────────────────────────────────────────

    @Operation(
            summary  = "Time-series axles data for one train (NDJSON)",
            responses = {
                    @ApiResponse(responseCode="200", description="Stream of AxlesDataDTO",
                            content=@Content(mediaType="application/x-ndjson",
                                    schema=@Schema(implementation=AxlesDataDTO.class))),
                    @ApiResponse(responseCode="400", description="Invalid parameters",
                            content=@Content(schema=@Schema(implementation=ApiError.class)))
            }
    )
    @GetMapping(value = "/data", produces = "application/x-ndjson")
    public Flux<AxlesDataDTO> getAxlesData(
            @ParameterObject @Validated AxlesDataRequest req
    ) {
        log.info("GET /axles/data {}", req);
        return axlesDataService
                .getAxlesData(req.getTrainNo(), req.getStart(), req.getEnd(), req.getMeasurementPoint())
                .doOnError(e -> log.error("Error fetching time-series axle data", e));
    }

    @Operation(
            summary  = "Global aggregated metrics for one TP",
            responses = {
                    @ApiResponse(responseCode="200", description="One AxlesDataDTO (aggregated)",
                            content=@Content(mediaType=MediaType.APPLICATION_JSON_VALUE,
                                    schema=@Schema(implementation=AxlesDataDTO.class))),
                    @ApiResponse(responseCode="400", description="Invalid TP",
                            content=@Content(schema=@Schema(implementation=ApiError.class)))
            }
    )
    @GetMapping("/global-aggregations")
    public Mono<AxlesDataDTO> getGlobalAggregations(
            @RequestParam("measurementPoint") @NotBlank String measurementPoint
    ) {
        log.info("GET /axles/global-aggregations tp={}", measurementPoint);
        return axlesDataService
                .getGlobalAggregations(measurementPoint)
                .doOnError(e -> log.error("Error fetching global aggregations", e));
    }

    //───────────────────────────────────────────────────────────────────────────────
    // 2) Per-train real-time SSE
    //───────────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Live SSE stream of axle readings from Kafka for one train/TP")
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AxlesDataDTO> streamAxlesData(
            @RequestParam @Min(1) Integer trainNo,
            @RequestParam @NotBlank String measurementPoint,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        LocalDateTime from = Optional.ofNullable(start).orElse(LocalDateTime.now().minusHours(1));
        LocalDateTime to   = Optional.ofNullable(end).orElse(LocalDateTime.now());
        log.info("Open SSE /axles/stream train={} tp={} window {}→{}", trainNo, measurementPoint, from, to);

        return ssePublisher.stream(trainNo, measurementPoint)
                .doOnNext(dto -> log.debug("SSE → {}", dto))
                .doOnError(err -> log.error("SSE error train={} tp={} : {}", trainNo, measurementPoint, err.getMessage()))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, SSE_RETRY_BACKOFF)
                        .maxBackoff(Duration.ofSeconds(60))
                        .filter(ex -> !(ex instanceof IllegalArgumentException))
                        .doBeforeRetry(sig -> log.warn("Retry SSE train={} tp={} because {}", trainNo, measurementPoint, sig.failure().getMessage()))
                );
    }

    //───────────────────────────────────────────────────────────────────────────────
    // 3) Station-wide endpoints
    //───────────────────────────────────────────────────────────────────────────────

    @Operation(summary = "List available stations")
    @GetMapping("/stations")
    public Flux<String> listStations() {
        return Flux.fromIterable(VALID_STATIONS);
    }

    @Operation(
            summary = "List train numbers for a given station",
            responses = {
                    @ApiResponse(responseCode="200", description="List of trainNos"),
                    @ApiResponse(responseCode="400", description="Unknown station",
                            content=@Content(schema=@Schema(implementation=ApiError.class)))
            }
    )
    @GetMapping("/stations/{station}/trains")
    public Flux<Integer> listTrainsByStation(
            @PathVariable("station") @NotBlank String station
    ) {
        String s = station.trim().toUpperCase();
        log.info("GET /axles/stations/{}/trains", s);

        if ("MP1".equals(s)) {
            return mp1HeaderRepo.findAll().map(HaugfjellMP1Header::getTrainNo);
        } else if ("MP3".equals(s)) {
            return mp3HeaderRepo.findAll().map(HaugfjellMP3Header::getTrainNo);
        } else {
            return Flux.error(new IllegalArgumentException("Unknown station: " + station));
        }
    }

    @Operation(
            summary     = "Historical raw readings for ALL trains in a station/TP (NDJSON)",
            description = "Stream between required start & end ISO-8601 timestamps"
    )
    @GetMapping(value = "/stations/{station}/history", produces = "application/x-ndjson")
    public Flux<AxlesDataDTO> getStationHistory(
            @PathVariable("station") @NotBlank String station,
            @RequestParam("measurementPoint") @NotBlank String measurementPoint,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        String s = station.trim().toUpperCase();
        log.info("GET /axles/stations/{}/history tp={} {}→{}", s, measurementPoint, start, end);

        Flux<Integer> trainNos;
        if ("MP1".equals(s)) {
            trainNos = mp1HeaderRepo.findAll().map(HaugfjellMP1Header::getTrainNo);
        } else if ("MP3".equals(s)) {
            trainNos = mp3HeaderRepo.findAll().map(HaugfjellMP3Header::getTrainNo);
        } else {
            return Flux.error(new IllegalArgumentException("Unknown station: " + station));
        }

        return trainNos.flatMap(trainNo ->
                axlesDataService.getAxlesData(trainNo, start, end, measurementPoint)
                        .doOnError(e -> log.error("Station history error [{}|{}]: {}", s, trainNo, e.getMessage()))
        );
    }

    @Operation(
            summary     = "Live SSE for ALL trains in a station/TP",
            description = "Server-Sent Events driven by Kafka messages"
    )
    @GetMapping(path = "/stations/{station}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AxlesDataDTO> streamStationAllTrains(
            @PathVariable("station") @NotBlank String station,
            @RequestParam("measurementPoint") @NotBlank String measurementPoint,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        String s = station.trim().toUpperCase();
        LocalDateTime from = Optional.ofNullable(start).orElse(LocalDateTime.now().minusHours(1));
        LocalDateTime to   = Optional.ofNullable(end).orElse(LocalDateTime.now());
        log.info("Open station SSE [{}] tp={} {}→{}", s, measurementPoint, from, to);

        Flux<Integer> trainNos;
        if ("MP1".equals(s)) {
            trainNos = mp1HeaderRepo.findAll().map(HaugfjellMP1Header::getTrainNo);
        } else if ("MP3".equals(s)) {
            trainNos = mp3HeaderRepo.findAll().map(HaugfjellMP3Header::getTrainNo);
        } else {
            return Flux.error(new IllegalArgumentException("Unknown station: " + station));
        }

        return trainNos.flatMap(trainNo ->
                ssePublisher.stream(trainNo, measurementPoint)
                        .doOnNext(dto -> log.debug("SSE [{}|{}] → {}", s, trainNo, dto))
                        .doOnError(err -> log.error("SSE error [{}|{}]: {}", s, trainNo, err.getMessage()))
                        .retryWhen(Retry.backoff(Long.MAX_VALUE, SSE_RETRY_BACKOFF)
                                .maxBackoff(Duration.ofSeconds(60))
                                .filter(ex -> !(ex instanceof IllegalArgumentException))
                                .doBeforeRetry(sig -> log.warn("Retry SSE [{}|{}]: {}", s, trainNo, sig.failure().getMessage()))
                        )
        );
    }

    //───────────────────────────────────────────────────────────────────────────────
    // 4) Trigger sensor-data aggregation over a time-range
    //───────────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Trigger aggregated sensor metrics",
            description = "Compute (and persist/cache) all sensor-type aggregates over the given range. Returns 200 on success."
    )
    @GetMapping("/aggregations")
    public Mono<ResponseEntity<Void>> triggerAggregations(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        log.info("→ GET /axles/aggregations {} → {}", from, to);
        // service expects ISO strings:
        String fromStr = from.toString();
        String toStr   = to.toString();

        return sensorDataAggregationService.aggregateSensorDataByRange(from, to)
                .then(Mono.fromSupplier(() -> {
                    log.info("← Aggregations complete {} → {}", from, to);
                    return ResponseEntity.ok().<Void>build();
                }))
                .onErrorResume(err -> {
                    log.error("Aggregation failed {} → {}", from, to, err);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}
