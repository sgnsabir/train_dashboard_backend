package com.banenor.service;

import com.banenor.dto.SensorAggregationDTO;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataServiceImpl implements SensorDataService {

    private final HaugfjellMP1AxlesRepository mp1Repo;
    private final HaugfjellMP3AxlesRepository mp3Repo;

    @Override
    public Mono<Void> processSensorData(String message) {
        log.info("Processing sensor data message: {}", message);
        // actual parsing & persistence logic goes here
        return Mono.empty();
    }

    @Override
    public Mono<Void> aggregateSensorDataByRange(String startDate, String endDate) {
        var fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var start = LocalDateTime.parse(startDate, fmt);
        var end   = LocalDateTime.parse(endDate, fmt);

        Flux<SensorAggregationDTO> mp1Flux = mp1Repo.aggregateSensorDataByRange(start, end)
                .doOnNext(r -> log.info("[MP1] vit={} avgSpeed={} minSpeed={} maxSpeed={}",
                        r.getVit(), r.getAvgSpeed(), r.getMinSpeed(), r.getMaxSpeed()));

        Flux<SensorAggregationDTO> mp3Flux = mp3Repo.aggregateSensorDataByRange(start, end)
                .doOnNext(r -> log.info("[MP3] vit={} avgSpeed={} minSpeed={} maxSpeed={}",
                        r.getVit(), r.getAvgSpeed(), r.getMinSpeed(), r.getMaxSpeed()));

        return Flux.concat(mp1Flux, mp3Flux)
                .then()
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> aggregateSensorData() {
        Mono<Double> mp1Speed = mp1Repo.findGlobalAverageSpeed().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP1] raw avg speed = {}", v));
        Mono<Double> mp3Speed = mp3Repo.findGlobalAverageSpeed().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP3] raw avg speed = {}", v));

        Mono<Double> mp1Aoa = mp1Repo.findGlobalAverageAoa().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP1] raw avg AOA   = {}", v));
        Mono<Double> mp3Aoa = mp3Repo.findGlobalAverageAoa().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP3] raw avg AOA   = {}", v));

        Mono<Double> mp1VibL = mp1Repo.findGlobalAverageVibrationLeft().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP1] raw avg vibL  = {}", v));
        Mono<Double> mp3VibL = mp3Repo.findGlobalAverageVibrationLeft().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP3] raw avg vibL  = {}", v));

        Mono<Double> mp1VibR = mp1Repo.findGlobalAverageVibrationRight().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP1] raw avg vibR  = {}", v));
        Mono<Double> mp3VibR = mp3Repo.findGlobalAverageVibrationRight().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP3] raw avg vibR  = {}", v));

        Mono<Double> mp1VertL = mp1Repo.findGlobalAverageVerticalForceLeft().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP1] raw avg vfrcl = {}", v));
        Mono<Double> mp3VertL = mp3Repo.findGlobalAverageVerticalForceLeft().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP3] raw avg vfrcl = {}", v));

        Mono<Double> mp1VertR = mp1Repo.findGlobalAverageVerticalForceRight().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP1] raw avg vfrcr = {}", v));
        Mono<Double> mp3VertR = mp3Repo.findGlobalAverageVerticalForceRight().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP3] raw avg vfrcr = {}", v));

        Mono<Double> mp1LatL = mp1Repo.findGlobalAverageLateralForceLeft().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP1] raw avg lfrcl = {}", v));
        Mono<Double> mp3LatL = mp3Repo.findGlobalAverageLateralForceLeft().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP3] raw avg lfrcl = {}", v));

        Mono<Double> mp1LatR = mp1Repo.findGlobalAverageLateralForceRight().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP1] raw avg lfrcr = {}", v));
        Mono<Double> mp3LatR = mp3Repo.findGlobalAverageLateralForceRight().defaultIfEmpty(0.0)
                .doOnNext(v -> log.info("[MP3] raw avg lfrcr = {}", v));

        return Mono.when(
                        mp1Speed, mp3Speed,
                        mp1Aoa,   mp3Aoa,
                        mp1VibL,  mp3VibL,
                        mp1VibR,  mp3VibR,
                        mp1VertL, mp3VertL,
                        mp1VertR, mp3VertR,
                        mp1LatL,  mp3LatL,
                        mp1LatR,  mp3LatR
                )
                .then()
                .subscribeOn(Schedulers.boundedElastic());
    }
}
