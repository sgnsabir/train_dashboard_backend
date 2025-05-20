package com.banenor.controller;

import com.banenor.dto.RawDataResponse;
import com.banenor.exception.ApiError;
import com.banenor.service.StationAxlesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/station/axles")
@CrossOrigin(origins = "${websocket.allowedOrigins}")
@RequiredArgsConstructor
@Validated
@Tag(name = "Station Axles", description = "Real-time and historical raw axle sensor data for MP1/MP3 or both")
public class StationAxlesController {

    private final StationAxlesService service;

    public enum Station { MP1, MP3, BOTH }

    private String stationName(Station station) {
        return station.name();
    }

    private Integer stationToTrainNo(Station station) {
        return switch (station) {
            case MP1  -> 1;
            case MP3  -> 3;
            case BOTH -> null;
        };
    }

    @Operation(
            summary     = "Stream real-time raw axle data (SSE)",
            description = "Server-Sent-Events of RawDataResponse as soon as new records appear.",
            parameters  = {
                    @Parameter(name = "station", in = ParameterIn.QUERY,
                            description = "MP1, MP3, or BOTH", required = true,
                            schema = @Schema(implementation = Station.class)),
                    @Parameter(name = "since", in = ParameterIn.QUERY,
                            description = "ISO-8601 timestamp; defaults to now",
                            schema = @Schema(type = "string", format = "date-time"))
            },
            responses   = {
                    @ApiResponse(responseCode = "200", description = "Live SSE of RawDataResponse",
                            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                                    schema = @Schema(implementation = RawDataResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid station",
                            content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "500", description = "Server error",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<RawDataResponse> streamRawData(
            @RequestParam Station station,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime since
    ) {
        Integer trainNo = stationToTrainNo(station);
        LocalDateTime from = since != null ? since : LocalDateTime.now();
        LocalDateTime to   = LocalDateTime.now();

        log.info("SSE /stream → station={}, trainNo={}, window=[{},{}]",
                station, trainNo, from, to);

        return service.streamRawData(
                        trainNo,
                        stationName(station),
                        from,
                        to
                )
                .doOnError(ex ->
                        log.error("Error in SSE stream for station={}", station, ex)
                );
    }

    @Operation(
            summary     = "Fetch historical raw axle data (NDJSON)",
            description = "All RawDataResponse DTOs between the given start and end.",
            parameters  = {
                    @Parameter(name = "station", in = ParameterIn.QUERY,
                            description = "MP1, MP3, or BOTH", required = true,
                            schema = @Schema(implementation = Station.class)),
                    @Parameter(name = "start",   in = ParameterIn.QUERY, required = true,
                            description = "ISO-8601 start timestamp",
                            schema = @Schema(type = "string", format = "date-time")),
                    @Parameter(name = "end",     in = ParameterIn.QUERY, required = true,
                            description = "ISO-8601 end timestamp",
                            schema = @Schema(type = "string", format = "date-time"))
            },
            responses   = {
                    @ApiResponse(responseCode = "200", description = "NDJSON of RawDataResponse",
                            content = @Content(mediaType = MediaType.APPLICATION_NDJSON_VALUE,
                                    schema = @Schema(implementation = RawDataResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid parameters",
                            content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "500", description = "Server error",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @GetMapping(value = "/data", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<RawDataResponse> getHistoricalData(
            @RequestParam Station station,
            @RequestParam @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,
            @RequestParam @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end
    ) {
        Integer trainNo = stationToTrainNo(station);
        log.info("GET /data → station={}, trainNo={}, window=[{},{}]",
                station, trainNo, start, end);

        return service.getHistoricalData(
                        trainNo,
                        stationName(station),
                        start,
                        end
                )
                .doOnError(ex ->
                        log.error("Error fetching historical data for station={}", station, ex)
                );
    }
}
