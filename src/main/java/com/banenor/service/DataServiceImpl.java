package com.banenor.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.banenor.dto.*;
import com.banenor.dto.RawDataResponse;
import com.banenor.mapper.AxleMapper;
import com.banenor.mapper.HaugfjellMP1Mapper;
import com.banenor.mapper.HaugfjellMP3Mapper;
import com.banenor.mapper.RawDataResponseFilter;
import com.banenor.mapper.SensorMeasurementMapper;
import com.banenor.model.AbstractAxles;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.repository.HaugfjellMP3HeaderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Slf4j
@Service
public class DataServiceImpl implements DataService {

    private static final Logger logger = LoggerFactory.getLogger(DataServiceImpl.class);
    private static final String DEAD_LETTER_TOPIC = "sensor-data-dead-letter";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(1);
    private static final Duration PROCESSING_TIMEOUT = Duration.ofSeconds(30);
    private static final AtomicInteger HEADER_ID_GENERATOR = new AtomicInteger(1);

    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Timer processingTimer;
    private final HaugfjellMP1HeaderRepository mp1HeaderRepo;
    private final HaugfjellMP1AxlesRepository    mp1AxlesRepo;
    private final HaugfjellMP3HeaderRepository mp3HeaderRepo;
    private final HaugfjellMP3AxlesRepository    mp3AxlesRepo;
    private final HaugfjellMP1Mapper             mp1Mapper;
    private final HaugfjellMP3Mapper             mp3Mapper;
    private final DashboardService               dashboardService;
    private final DigitalTwinService             digitalTwinService;
    private final AxleMapper                     axleMapper;
    private final RawDataResponseFilter          rawDataResponseFilter;
    private final SensorMeasurementMapper        sensorMeasurementMapper;

    public DataServiceImpl(
            HaugfjellMP1HeaderRepository mp1HeaderRepo,
            HaugfjellMP1AxlesRepository mp1AxlesRepo,
            HaugfjellMP3HeaderRepository mp3HeaderRepo,
            HaugfjellMP3AxlesRepository mp3AxlesRepo,
            AxleMapper axleMapper,
            RawDataResponseFilter rawDataResponseFilter,
            SensorMeasurementMapper sensorMeasurementMapper,
            HaugfjellMP1Mapper mp1Mapper,
            HaugfjellMP3Mapper mp3Mapper,
            DigitalTwinService digitalTwinService,
            DashboardService dashboardService,
            MeterRegistry meterRegistry
    ) {
        this.mp1HeaderRepo           = mp1HeaderRepo;
        this.mp1AxlesRepo            = mp1AxlesRepo;
        this.mp3HeaderRepo           = mp3HeaderRepo;
        this.mp3AxlesRepo            = mp3AxlesRepo;
        this.axleMapper              = axleMapper;
        this.rawDataResponseFilter   = rawDataResponseFilter;
        this.sensorMeasurementMapper = sensorMeasurementMapper;
        this.mp1Mapper               = mp1Mapper;
        this.mp3Mapper               = mp3Mapper;
        this.dashboardService        = dashboardService;
        this.digitalTwinService      = digitalTwinService;
        this.meterRegistry           = meterRegistry;
        this.objectMapper            = new ObjectMapper().registerModule(new JavaTimeModule());
        this.processingTimer         = Timer.builder("sensor.data.processing")
                .description("Time taken to process sensor data")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    @Override
    @Transactional
    public Mono<Void> processSensorData(String message) {
        return Mono.defer(() -> processingTimer.record(() -> {
            meterRegistry.counter("sensor.data.received").increment();

            return Mono.fromCallable(() -> objectMapper.readValue(message, SensorDataPayload.class))
                    .flatMap(payload -> {
                        var headerDTO      = payload.getHeader();
                        var measurementDTO = payload.getMeasurement();
                        String mplace      = headerDTO.getMplace();

                        if (mplace == null || mplace.trim().isEmpty()) {
                            meterRegistry.counter("sensor.data.validation.errors", "type", "missing_mplace").increment();
                            return Mono.error(new IllegalArgumentException("Measurement station (mplace) is null or empty"));
                        }
                        if (headerDTO.getCooLat() == null || headerDTO.getCooLong() == null) {
                            meterRegistry.counter("sensor.data.validation.errors", "type", "missing_coordinates").increment();
                            logger.error("Required coordinates missing for station: {}", mplace);
                            return Mono.empty();
                        }

                        Mono<Integer> trainNoMono = switch (mplace.toUpperCase()) {
                            case "MP1" -> processMP1Data(headerDTO, measurementDTO);
                            case "MP3" -> processMP3Data(headerDTO, measurementDTO);
                            default   -> {
                                meterRegistry.counter("sensor.data.validation.errors", "type", "invalid_mplace").increment();
                                yield Mono.error(new IllegalArgumentException("Unknown measurement station: " + mplace));
                            }
                        };

                        return trainNoMono
                                .flatMap(trainNo -> dashboardService.getLatestMetrics(trainNo)
                                        .doOnNext(metrics -> meterRegistry.counter("sensor.data.metrics.updated").increment())
                                        .flatMap(metrics -> digitalTwinService.updateTwin(metrics)
                                                .doOnSuccess(v -> meterRegistry.counter("digital.twin.updated").increment())))
                                .timeout(PROCESSING_TIMEOUT)
                                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_DELAY)
                                        .doBeforeRetry(sig -> logger.warn("Retrying after error: {}", sig.failure().getMessage())))
                                .then();
                    })
                    .onErrorResume(ex -> {
                        logger.error("Failed to process sensor data: {}", ex.getMessage(), ex);
                        meterRegistry.counter("sensor.data.errors", "type", ex.getClass().getSimpleName()).increment();
                        logger.info("Message sent to dead-letter topic: {}", DEAD_LETTER_TOPIC);
                        return Mono.empty();
                    })
                    .then();
        })).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Integer> processMP1Data(AnalysisHeaderDTO headerDTO, AnalysisMeasurementDTO measurementDTO) {
        return mp1HeaderRepo.findByMstartTime(headerDTO.getMstartTime())
                .switchIfEmpty(Mono.defer(() -> {
                    var newHeader = mp1Mapper.headerDtoToEntity(headerDTO);
                    if (newHeader.getTrainNo() == null) {
                        newHeader.setTrainNo(HEADER_ID_GENERATOR.getAndIncrement());
                    }
                    return mp1HeaderRepo.save(newHeader);
                }))
                .flatMap(h -> mp1AxlesRepo.save(mp1Mapper.measurementDtoToEntity(measurementDTO, h))
                        .thenReturn(h.getTrainNo()))
                .doOnSuccess(n -> meterRegistry.counter("sensor.data.processed", "type", "MP1").increment());
    }

    private Mono<Integer> processMP3Data(AnalysisHeaderDTO headerDTO, AnalysisMeasurementDTO measurementDTO) {
        return mp3HeaderRepo.findByMstartTime(headerDTO.getMstartTime())
                .switchIfEmpty(Mono.defer(() -> {
                    var newHeader = mp3Mapper.headerDtoToEntity(headerDTO);
                    if (newHeader.getTrainNo() == null) {
                        newHeader.setTrainNo(HEADER_ID_GENERATOR.getAndIncrement());
                    }
                    return mp3HeaderRepo.save(newHeader);
                }))
                .flatMap(h -> mp3AxlesRepo.save(mp3Mapper.measurementDtoToEntity(measurementDTO, h))
                        .thenReturn(h.getTrainNo()))
                .doOnSuccess(n -> meterRegistry.counter("sensor.data.processed", "type", "MP3").increment());
    }

    @Override
    public Flux<RawDataResponse> getRawData(Integer analysisId, String sensorType, int page, int size) {
        if (analysisId == null || analysisId <= 0) {
            return Flux.error(new IllegalArgumentException("Invalid analysisId"));
        }
        if (page < 0 || size <= 0) {
            return Flux.error(new IllegalArgumentException("Invalid pagination parameters"));
        }

        meterRegistry.counter("raw.data.requests").increment();
        Timer.Sample sample = Timer.start(meterRegistry);

        Flux<AbstractAxles> axlesFlux = mp1HeaderRepo.findById(analysisId)
                .flatMapMany(h -> mp1AxlesRepo.findByTrainNo(analysisId).cast(AbstractAxles.class))
                .switchIfEmpty(mp3HeaderRepo.findById(analysisId)
                        .flatMapMany(h -> mp3AxlesRepo.findByTrainNo(analysisId).cast(AbstractAxles.class)))
                .switchIfEmpty(Flux.empty());

        return axlesFlux
                .map(axleMapper::toRawDataResponse)
                .map(dto -> rawDataResponseFilter.filter(dto, sensorType))
                .skip((long) page * size)
                .take(size)
                .doOnComplete(() -> {
                    sample.stop(meterRegistry.timer("raw.data.processing.time"));
                    meterRegistry.counter("raw.data.success").increment();
                })
                .doOnError(e -> {
                    meterRegistry.counter("raw.data_errors", "type", e.getClass().getSimpleName()).increment();
                    logger.error("Error retrieving raw data: {}", e.getMessage(), e);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<RawDataResponse> getDetailedSensorData(Integer analysisId, int page, int size) {
        if (analysisId == null || analysisId <= 0) {
            return Flux.error(new IllegalArgumentException("Invalid analysisId"));
        }
        if (page < 0 || size <= 0) {
            return Flux.error(new IllegalArgumentException("Invalid pagination parameters"));
        }

        meterRegistry.counter("detailed.data.requests").increment();
        Timer.Sample sample = Timer.start(meterRegistry);

        Flux<AbstractAxles> axlesFlux = mp1HeaderRepo.findById(analysisId)
                .flatMapMany(h -> mp1AxlesRepo.findByTrainNo(analysisId).cast(AbstractAxles.class))
                .switchIfEmpty(mp3HeaderRepo.findById(analysisId)
                        .flatMapMany(h -> mp3AxlesRepo.findByTrainNo(analysisId).cast(AbstractAxles.class)))
                .switchIfEmpty(Flux.empty());

        return axlesFlux
                .map(sensorMeasurementMapper::toSensorMeasurementDTO)
                .skip((long) page * size)
                .take(size)
                .doOnComplete(() -> {
                    sample.stop(meterRegistry.timer("detailed.data.processing.time"));
                    meterRegistry.counter("detailed.data.success").increment();
                })
                .doOnError(e -> {
                    meterRegistry.counter("detailed.data.errors", "type", e.getClass().getSimpleName()).increment();
                    logger.error("Error retrieving detailed sensor data: {}", e.getMessage(), e);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    // inside DataServiceImpl.java

    @Override
    public Mono<SensorTpSeriesDTO> tpSeries(Integer analysisId,
                                            String station,
                                            LocalDateTime start,
                                            LocalDateTime end,
                                            String sensor) {

        SensorField field = SensorField.fromCode(sensor);   // validates sensor keyword
        Flux<RawDataResponse> rows;

        if (analysisId != null) {
            rows = getDetailedSensorData(analysisId, 0, Integer.MAX_VALUE);
        } else {
            Flux<AbstractAxles> ax;
            switch (station.toUpperCase()) {
                case "MP1" ->
                        ax = mp1AxlesRepo.findAllByCreatedAtBetween(start, end).cast(AbstractAxles.class);
                case "MP3" ->
                        ax = mp3AxlesRepo.findAllByCreatedAtBetween(start, end).cast(AbstractAxles.class);
                case "BOTH" ->
                        ax = Flux.merge(
                                mp1AxlesRepo.findAllByCreatedAtBetween(start, end),
                                mp3AxlesRepo.findAllByCreatedAtBetween(start, end)
                        ).cast(AbstractAxles.class);
                default  ->
                { return Mono.error(new IllegalArgumentException("Unknown station " + station)); }
            }
            rows = ax.map(axleMapper::toRawDataResponse);
        }

        return rows.collectList().map(list -> {
            Map<Integer,List<Double>> map = Map.of(
                    1, new ArrayList<>(), 2, new ArrayList<>(), 3, new ArrayList<>(),
                    5, new ArrayList<>(), 6, new ArrayList<>(), 8, new ArrayList<>()
            );

            list.forEach(dto -> {
                List<Double> vals = field.extract(dto);        // six-element list
                int i = 0;
                for (int tp : List.of(1,2,3,5,6,8)) {
                    Double v = vals.get(i++);
                    if (v != null) map.get(tp).add(v);
                }
            });

            return new SensorTpSeriesDTO(
                    analysisId,
                    sensor,
                    List.of(1,2,3,5,6,8),
                    map
            );
        });
    }


}
