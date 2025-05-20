package com.banenor.service;

import com.banenor.dto.RawDataResponse;
import com.banenor.dto.AxlesDataDTO;
import com.banenor.mapper.AxleMapper;
import com.banenor.model.AbstractAxles;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.util.RepositoryResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationAxlesServiceImpl implements StationAxlesService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final RepositoryResolver               repositoryResolver;
    private final HaugfjellMP1AxlesRepository     mp1Repo;
    private final HaugfjellMP3AxlesRepository     mp3Repo;
    private final AxleMapper                      axleMapper;
    private final KafkaSsePublisherService         kafkaPublisher;
    private final ReactiveRedisTemplate<String,Object> redisTemplate;

    @Override
    public Flux<RawDataResponse> getRawAxlesData(Integer trainNo,
                                                 LocalDateTime start,
                                                 LocalDateTime end) {
        String cacheKey = String.format(
                "rawAxles:%s:%s:%s",
                Optional.ofNullable(trainNo).map(Object::toString).orElse("ALL"),
                start, end
        );

        ReactiveValueOperations<String,Object> ops = redisTemplate.opsForValue();

        return ops.get(cacheKey)
                .cast(List.class)
                .flatMapMany(cached -> {
                    @SuppressWarnings("unchecked")
                    List<RawDataResponse> list = (List<RawDataResponse>) cached;
                    log.debug("CACHE HIT [{}] → {} records", cacheKey, list.size());
                    return Flux.fromIterable(list);
                })
                .switchIfEmpty(
                        repositoryResolver.resolveRepository(trainNo)
                                .flatMapMany(repo -> {
                                    try {
                                        Method m = repo.getClass()
                                                .getMethod("findByTrainNoAndCreatedAtBetween",
                                                        Integer.class, LocalDateTime.class, LocalDateTime.class);
                                        return ((Flux<AbstractAxles>) m.invoke(repo, trainNo, start, end));
                                    } catch (NoSuchMethodException e) {
                                        return ((Flux<AbstractAxles>) repo.findAll())
                                                .filter(a -> !a.getCreatedAt().isBefore(start) && !a.getCreatedAt().isAfter(end));
                                    } catch (Exception e) {
                                        return Flux.error(new IllegalStateException(
                                                "Failed to invoke date‐range fetch", e));
                                    }
                                })
                                .cast(AbstractAxles.class)
                                .map(axleMapper::toRawDataResponse)
                                .collectList()
                                .flatMapMany(list ->
                                        ops.set(cacheKey, list, CACHE_TTL)
                                                .doOnSuccess(ok -> {
                                                    if (Boolean.TRUE.equals(ok)) {
                                                        log.debug("Cached [{}] → {} records (TTL {}s)",
                                                                cacheKey, list.size(), CACHE_TTL.getSeconds());
                                                    }
                                                })
                                                .thenMany(Flux.fromIterable(list))
                                )
                )
                .doOnError(e -> log.error("Error in getRawAxlesData [{}]:", cacheKey, e));
    }

    @Override
    public Flux<RawDataResponse> getHistoricalData(Integer trainNo,
                                                   String station,
                                                   LocalDateTime start,
                                                   LocalDateTime end) {
        return resolveStationFlux(trainNo, station, start, end)
                .map(axleMapper::toRawDataResponse)
                .doOnError(e -> log.error(
                        "Error fetching historical data train={}, station={}, window=[{},{}]",
                        trainNo, station, start, end, e));
    }

    @Override
    public Flux<RawDataResponse> streamRawData(Integer trainNo,
                                               String station,
                                               LocalDateTime start,
                                               LocalDateTime end) {
        Flux<RawDataResponse> history = getHistoricalData(trainNo, station, start, end);

        Flux<RawDataResponse> live = kafkaPublisher.stream(trainNo, station)
                .map(this::toRawDataResponse)
                .doOnError(e -> log.error(
                        "Error in live stream train={}, station={}", trainNo, station, e));

        log.info("Starting historical+live stream train={}, station={}, window=[{},{}]",
                trainNo, station, start, end);

        return Flux.concat(history, live);
    }

    private Flux<AbstractAxles> resolveStationFlux(Integer trainNo,
                                                   String station,
                                                   LocalDateTime start,
                                                   LocalDateTime end) {
        boolean both = station == null
                || station.isBlank()
                || station.equalsIgnoreCase("both");

        if (both) {
            log.debug("HISTORICAL → BOTH MP1 & MP3 (train={}, window=[{},{}])",
                    trainNo, start, end);
            return Flux.merge(
                    mp1Repo.findByTrainNoAndCreatedAtBetween(trainNo, start, end)
                            .cast(AbstractAxles.class),
                    mp3Repo.findByTrainNoAndCreatedAtBetween(trainNo, start, end)
                            .cast(AbstractAxles.class)
            );
        }

        if (station.equalsIgnoreCase("MP1")) {
            log.debug("HISTORICAL → MP1 (train={}, window=[{},{}])", trainNo, start, end);
            return mp1Repo.findByTrainNoAndCreatedAtBetween(trainNo, start, end)
                    .cast(AbstractAxles.class);
        }

        if (station.equalsIgnoreCase("MP3")) {
            log.debug("HISTORICAL → MP3 (train={}, window=[{},{}])", trainNo, start, end);
            return mp3Repo.findByTrainNoAndCreatedAtBetween(trainNo, start, end)
                    .cast(AbstractAxles.class);
        }

        String err = "Unknown station '" + station + "'; expected MP1, MP3 or both";
        log.error(err);
        return Flux.error(new IllegalArgumentException(err));
    }

    private RawDataResponse toRawDataResponse(AxlesDataDTO dto) {
        RawDataResponse r = new RawDataResponse();
        r.setAnalysisId(dto.getTrainNo());
        r.setCreatedAt(dto.getCreatedAt());
        String tp = dto.getMeasurementPoint().toLowerCase();

        switch (tp) {
            case "tp1":
                r.setSpdTp1(dto.getSpeed());
                r.setAoaTp1(dto.getAngleOfAttack());
                r.setVviblTp1(dto.getVibrationLeft());
                r.setVvibrTp1(dto.getVibrationRight());
                r.setVfrclTp1(dto.getVerticalForceLeft());
                r.setVfrcrTp1(dto.getVerticalForceRight());
                r.setLfrclTp1(dto.getLateralForceLeft());
                r.setLfrcrTp1(dto.getLateralForceRight());
                r.setLviblTp1(dto.getLateralVibrationLeft());
                r.setLvibrTp1(dto.getLateralVibrationRight());
                break;

            case "tp2":
                r.setSpdTp2(dto.getSpeed());
                r.setAoaTp2(dto.getAngleOfAttack());
                r.setVviblTp2(dto.getVibrationLeft());
                r.setVvibrTp2(dto.getVibrationRight());
                r.setVfrclTp2(dto.getVerticalForceLeft());
                r.setVfrcrTp2(dto.getVerticalForceRight());
                r.setLfrclTp2(dto.getLateralForceLeft());
                r.setLfrcrTp2(dto.getLateralForceRight());
                r.setLviblTp2(dto.getLateralVibrationLeft());
                r.setLvibrTp2(dto.getLateralVibrationRight());
                break;

            case "tp3":
                r.setSpdTp3(dto.getSpeed());
                r.setAoaTp3(dto.getAngleOfAttack());
                r.setVviblTp3(dto.getVibrationLeft());
                r.setVvibrTp3(dto.getVibrationRight());
                r.setVfrclTp3(dto.getVerticalForceLeft());
                r.setVfrcrTp3(dto.getVerticalForceRight());
                r.setLfrclTp3(dto.getLateralForceLeft());
                r.setLfrcrTp3(dto.getLateralForceRight());
                r.setLviblTp3(dto.getLateralVibrationLeft());
                r.setLvibrTp3(dto.getLateralVibrationRight());
                break;

            case "tp5":
                r.setSpdTp5(dto.getSpeed());
                r.setAoaTp5(dto.getAngleOfAttack());
                r.setVviblTp5(dto.getVibrationLeft());
                r.setVvibrTp5(dto.getVibrationRight());
                r.setVfrclTp5(dto.getVerticalForceLeft());
                r.setVfrcrTp5(dto.getVerticalForceRight());
                r.setLfrclTp5(dto.getLateralForceLeft());
                r.setLfrcrTp5(dto.getLateralForceRight());
                r.setLviblTp5(dto.getLateralVibrationLeft());
                r.setLvibrTp5(dto.getLateralVibrationRight());
                break;

            case "tp6":
                r.setSpdTp6(dto.getSpeed());
                r.setAoaTp6(dto.getAngleOfAttack());
                r.setVviblTp6(dto.getVibrationLeft());
                r.setVvibrTp6(dto.getVibrationRight());
                r.setVfrclTp6(dto.getVerticalForceLeft());
                r.setVfrcrTp6(dto.getVerticalForceRight());
                r.setLfrclTp6(dto.getLateralForceLeft());
                r.setLfrcrTp6(dto.getLateralForceRight());
                r.setLviblTp6(dto.getLateralVibrationLeft());
                r.setLvibrTp6(dto.getLateralVibrationRight());
                break;

            case "tp8":
                r.setSpdTp8(dto.getSpeed());
                r.setAoaTp8(dto.getAngleOfAttack());
                r.setVviblTp8(dto.getVibrationLeft());
                r.setVvibrTp8(dto.getVibrationRight());
                r.setVfrclTp8(dto.getVerticalForceLeft());
                r.setVfrcrTp8(dto.getVerticalForceRight());
                // note: lfrclTp8 and lfrcrTp8 do not exist on model
                // same for lviblTp8 / lvibrTp8
                break;

            default:
                log.warn("Unexpected measurementPoint in live DTO: {}", dto.getMeasurementPoint());
        }

        // tag which TP this came from
        r.setSensorType(dto.getMeasurementPoint());
        r.setValue(dto.getSpeed()); // or whichever you prefer to surface as 'value'

        return r;
    }

}
