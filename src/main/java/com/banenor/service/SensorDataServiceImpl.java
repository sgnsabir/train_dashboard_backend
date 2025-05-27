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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataServiceImpl implements SensorDataService {

    private final HaugfjellMP1AxlesRepository mp1Repo;
    private final HaugfjellMP3AxlesRepository mp3Repo;

    // ── 1) incoming message parsing ───────────────
    @Override
    public Mono<Void> processSensorData(String message) {
        log.info("Processing sensor data message: {}", message);
        // TODO: parse JSON → entities → save
        return Mono.empty();
    }

    // ── 2) time-bounded aggregation ────────────────
    @Override
    public Mono<Void> aggregateSensorDataByRange(LocalDateTime from, LocalDateTime to) {
        log.info("Triggering aggregation from {} → {}", from, to);

        Flux<SensorAggregationDTO> mp1Flux = mp1Repo.aggregateSensorDataByRange(from, to)
                .doOnNext(r -> log.debug("[MP1 {}] avg={} min={} max={}",
                        r.getVit(), r.getAverageSpeed(), r.getMinSpeed(), r.getMaxSpeed()));

        Flux<SensorAggregationDTO> mp3Flux = mp3Repo.aggregateSensorDataByRange(from, to)
                .doOnNext(r -> log.debug("[MP3 {}] avg={} min={} max={}",
                        r.getVit(), r.getAverageSpeed(), r.getMinSpeed(), r.getMaxSpeed()));

        return Flux.concat(mp1Flux, mp3Flux)
                .then()
                .doOnSuccess(v -> log.info("Completed aggregation {} → {}", from, to))
                .subscribeOn(Schedulers.boundedElastic());
    }

    // ── 3) global, non-bounded aggregation ─────────
    @Override
    public Mono<Void> aggregateSensorData() {
        log.info("Triggering GLOBAL aggregation (no time window)");

        Mono<Double> mp1Speed = mp1Repo.findGlobalAverageSpeed().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP1] global avg speed = {}", v));
        Mono<Double> mp3Speed = mp3Repo.findGlobalAverageSpeed().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP3] global avg speed = {}", v));

        Mono<Double> mp1Aoa = mp1Repo.findGlobalAverageAoa().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP1] global avg aoa   = {}", v));
        Mono<Double> mp3Aoa = mp3Repo.findGlobalAverageAoa().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP3] global avg aoa   = {}", v));

        Mono<Double> mp1VibL = mp1Repo.findGlobalAverageVibrationLeft().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP1] global avg vibL  = {}", v));
        Mono<Double> mp3VibL = mp3Repo.findGlobalAverageVibrationLeft().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP3] global avg vibL  = {}", v));

        Mono<Double> mp1VibR = mp1Repo.findGlobalAverageVibrationRight().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP1] global avg vibR  = {}", v));
        Mono<Double> mp3VibR = mp3Repo.findGlobalAverageVibrationRight().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP3] global avg vibR  = {}", v));

        Mono<Double> mp1VertL = mp1Repo.findGlobalAverageVerticalForceLeft().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP1] global avg vfrcl = {}", v));
        Mono<Double> mp3VertL = mp3Repo.findGlobalAverageVerticalForceLeft().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP3] global avg vfrcl = {}", v));

        Mono<Double> mp1VertR = mp1Repo.findGlobalAverageVerticalForceRight().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP1] global avg vfrcr = {}", v));
        Mono<Double> mp3VertR = mp3Repo.findGlobalAverageVerticalForceRight().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP3] global avg vfrcr = {}", v));

        Mono<Double> mp1LatL = mp1Repo.findGlobalAverageLateralForceLeft().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP1] global avg lfrcl = {}", v));
        Mono<Double> mp3LatL = mp3Repo.findGlobalAverageLateralForceLeft().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP3] global avg lfrcl = {}", v));

        Mono<Double> mp1LatR = mp1Repo.findGlobalAverageLateralForceRight().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP1] global avg lfrcr = {}", v));
        Mono<Double> mp3LatR = mp3Repo.findGlobalAverageLateralForceRight().defaultIfEmpty(0.0)
                .doOnNext(v -> log.debug("[MP3] global avg lfrcr = {}", v));

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

    // ── 4) raw time-series fetch ──────────────────
    @Override
    public Flux<Object> getSensorDataByRange(LocalDateTime from, LocalDateTime to) {
        log.debug("Fetching raw sensor data {} → {}", from, to);
        return Flux.concat(
                        mp1Repo.findByCreatedAtBetween(from, to),
                        mp3Repo.findByCreatedAtBetween(from, to)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }

    // ── 5a) performance over entire span ─────────
    @Override
    public Mono<SensorAggregationDTO> getPerformance(Integer analysisId) {
        // delegate to the new time-windowed method with full span
        return getPerformance(analysisId, LocalDateTime.MIN, LocalDateTime.MAX);
    }

    // ── 5b) performance over arbitrary [from→to] ─
    @Override
    public Mono<SensorAggregationDTO> getPerformance(Integer analysisId,
                                                     LocalDateTime from,
                                                     LocalDateTime to) {
        log.info("Calculating Performance Index for {} within {} → {}", analysisId, from, to);

        // 1) static averages (speed, aoa, forces, vibrations) within window
        Mono<Double> avgSpeed = Flux.concat(
                        mp1Repo.findByTrainNoAndCreatedAtBetween(analysisId, from, to),
                        mp3Repo.findByTrainNoAndCreatedAtBetween(analysisId, from, to)
                )
                .map(r -> (r.getSpdTp1() + r.getSpdTp2() + r.getSpdTp3() + r.getSpdTp5() + r.getSpdTp6() + r.getSpdTp8()) / 6.0)
                .collectList()
                .map(list -> list.stream().mapToDouble(d -> d).average().orElse(0.0))
                .doOnNext(v -> log.debug("Avg speed = {}", v));

        Mono<Double> avgAoa = Flux.concat(
                        mp1Repo.findByTrainNoAndCreatedAtBetween(analysisId, from, to),
                        mp3Repo.findByTrainNoAndCreatedAtBetween(analysisId, from, to)
                )
                .map(r -> (r.getAoaTp1() + r.getAoaTp2() + r.getAoaTp3() + r.getAoaTp5() + r.getAoaTp6() + r.getAoaTp8()) / 6.0)
                .collectList()
                .map(list -> list.stream().mapToDouble(d -> d).average().orElse(0.0))
                .doOnNext(v -> log.debug("Avg AOA   = {}", v));

        // 2) vertical & lateral force
        Mono<Double> vertForce = Flux.concat(
                        mp1Repo.findByTrainNoAndCreatedAtBetween(analysisId, from, to),
                        mp3Repo.findByTrainNoAndCreatedAtBetween(analysisId, from, to)
                )
                .map(r -> (r.getVfrclTp1() + r.getVfrclTp2() + r.getVfrclTp3() + r.getVfrclTp5() + r.getVfrclTp6() + r.getVfrclTp8()
                        + r.getVfrcrTp1() + r.getVfrcrTp2() + r.getVfrcrTp3() + r.getVfrcrTp5() + r.getVfrcrTp6() + r.getVfrcrTp8()) / 12.0)
                .collectList()
                .map(list -> list.stream().mapToDouble(d -> d).average().orElse(0.0))
                .doOnNext(v -> log.debug("Avg vertical force = {}", v));

        Mono<Double> latForce = Flux.concat(
                        mp1Repo.findByTrainNoAndCreatedAtBetween(analysisId, from, to),
                        mp3Repo.findByTrainNoAndCreatedAtBetween(analysisId, from, to)
                )
                .map(r -> (r.getLfrclTp1() + r.getLfrclTp2() + r.getLfrclTp3() + r.getLfrclTp5() + r.getLfrclTp6()
                        + r.getLfrcrTp1() + r.getLfrcrTp2() + r.getLfrcrTp3() + r.getLfrcrTp5() + r.getLfrcrTp6()) / 10.0)
                .collectList()
                .map(list -> list.stream().mapToDouble(d -> d).average().orElse(0.0))
                .doOnNext(v -> log.debug("Avg lateral force  = {}", v));

        // 3) ride RMS
        Mono<Double> rideRms = Flux.concat(
                        mp1Repo.findByTrainNoAndCreatedAtBetween(analysisId, from, to),
                        mp3Repo.findByTrainNoAndCreatedAtBetween(analysisId, from, to)
                )
                .map(r -> (r.getVviblTp1() + r.getVviblTp2() + r.getVviblTp3() + r.getVviblTp5() + r.getVviblTp6() + r.getVviblTp8()
                        + r.getVvibrTp1() + r.getVvibrTp2() + r.getVvibrTp3() + r.getVvibrTp5() + r.getVvibrTp6() + r.getVvibrTp8()) / 12.0)
                .collectList()
                .map(list -> list.stream().mapToDouble(d -> d).average().orElse(0.0))
                .doOnNext(v -> log.debug("Ride RMS           = {}", v));

        // 4) mean positive acceleration
        record TP(LocalDateTime ts, Double speed) {}
        Flux<TP> speedSeries = Flux.merge(
                        mp1Repo.findByTrainNoAndCreatedAtBetween(analysisId, from, to)
                                .map(r -> new TP(r.getCreatedAt(), (r.getSpdTp1() + r.getSpdTp2() + r.getSpdTp3() + r.getSpdTp5() + r.getSpdTp6() + r.getSpdTp8()) / 6.0)),
                        mp3Repo.findByTrainNoAndCreatedAtBetween(analysisId, from, to)
                                .map(r -> new TP(r.getCreatedAt(), (r.getSpdTp1() + r.getSpdTp2() + r.getSpdTp3() + r.getSpdTp5() + r.getSpdTp6() + r.getSpdTp8()) / 6.0))
                )
                .sort(Comparator.comparing(tp -> tp.ts));

        Mono<Double> meanPositiveAccel = speedSeries
                .buffer(2,1)
                .filter(b -> b.size() == 2)
                .map(b -> {
                    TP a = b.get(0), c = b.get(1);
                    double dt = Duration.between(a.ts, c.ts).toMillis() / 1_000.0;
                    return dt > 0 ? (c.speed - a.speed) / dt : 0.0;
                })
                .filter(x -> x > 0)
                .collectList()
                .map(list -> list.stream().mapToDouble(d -> d).average().orElse(0.0))
                .doOnNext(v -> log.debug("Mean +ve accel     = {}", v));

        // 5) L/V ratio
        Mono<Double> lvRatio = latForce.zipWith(vertForce, (lf, vf) -> vf > 0 ? lf / vf : 0.0)
                .doOnNext(v -> log.debug("L/V ratio          = {}", v));

        // 6) combine & normalize
        return Mono.zip(avgSpeed, avgAoa, vertForce, latForce, rideRms, lvRatio)
                .zipWith(meanPositiveAccel)
                .map(t -> {
                    var six = t.getT1();
                    double accel = t.getT2();

                    SensorAggregationDTO dto = new SensorAggregationDTO();
                    dto.setAverageSpeed(six.getT1());
                    dto.setAverageAoa(six.getT2());
                    dto.setAverageVerticalForceLeft(six.getT3() / 2);
                    dto.setAverageVerticalForceRight(six.getT3() / 2);
                    dto.setAverageLateralForceLeft(six.getT4() / 2);
                    dto.setAverageLateralForceRight(six.getT4() / 2);
                    dto.setAverageVibrationLeft(six.getT5() / 2);
                    dto.setAverageVibrationRight(six.getT5() / 2);

                    dto.setMeanPositiveAccel(accel);
                    dto.setRideRms(six.getT5());
                    dto.setLvRatio(six.getT6());

                    double pi = 100 * (
                            0.30 * clamp(six.getT1() / 20.0)         // speed
                                    + 0.15 * clamp(3.0 / Math.abs(six.getT2()))  // AOA
                                    + 0.10 * clamp(1.0 / six.getT5())           // rideRMS
                                    + 0.15 * clamp(0.4 / six.getT6())           // L/V
                                    + 0.10 * 1.0                                 // placeholder accel
                    );
                    dto.setPerformanceIndex(clamp(pi / 100) * 100);

                    log.info("PI[{} {}→{}] = {}", analysisId, from, to, dto.getPerformanceIndex());
                    return dto;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    // ─── clamp helper ─────────────────────────────
    private static double clamp(double x) {
        return x < 0 ? 0 : x > 1 ? 1 : x;
    }
}
