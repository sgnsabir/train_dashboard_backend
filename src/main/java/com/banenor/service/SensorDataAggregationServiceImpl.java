package com.banenor.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.banenor.dto.SensorAggregationDTO;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataAggregationServiceImpl implements SensorDataAggregationService {

    private final HaugfjellMP1AxlesRepository mp1Repo;
    private final HaugfjellMP3AxlesRepository mp3Repo;

    /**
     * Aggregates sensor data for an arbitrary date range.
     */
    @Override
    public Mono<Void> aggregateSensorDataByRange(String startDate, String endDate) {
        return aggregateSensorDataByRangeReactive(startDate, endDate);
    }

    private Mono<Void> aggregateSensorDataByRangeReactive(String startDate, String endDate) {
        // Parse ISO-8601 date-time inputs, e.g. "2025-05-01T00:00:00"
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(startDate, formatter);
        LocalDateTime end   = LocalDateTime.parse(endDate,   formatter);

        Flux<SensorAggregationDTO> mp1Aggregation = mp1Repo.aggregateSensorDataByRange(start, end);
        Flux<SensorAggregationDTO> mp3Aggregation = mp3Repo.aggregateSensorDataByRange(start, end);

        return Flux.concat(mp1Aggregation, mp3Aggregation)
                .collectList()
                .doOnNext(list -> list.forEach(dto ->
                        log.info("Aggregated Result → vit: {}, avgSpeed: {}, minSpeed: {}, maxSpeed: {}",
                                dto.getVit(), dto.getAvgSpeed(), dto.getMinSpeed(), dto.getMaxSpeed())
                ))
                .then()
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Aggregates global sensor data on a fixed schedule.
     */
    @Override
    public Mono<Void> aggregateSensorData() {
        return aggregateSensorDataReactive();
    }

    private Mono<Void> aggregateSensorDataReactive() {
        // Monos order:
        //  0–9   MP1 averages: speed, aoa, vibL, vibR, vfrcl, vfrcr, lfrcl, lfrcr, lvibl, lvibr
        //  10    MP1 avg square speed
        // 11–20  MP3 averages
        // 21     MP3 avg square speed
        List<Mono<Double>> sources = List.of(
                mp1Repo.findGlobalAverageSpeed().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageAoa().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageVibrationLeft().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageVibrationRight().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageVerticalForceLeft().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageVerticalForceRight().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageLateralForceLeft().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageLateralForceRight().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageLateralVibrationLeft().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageLateralVibrationRight().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageSquareSpeed().defaultIfEmpty(0.0),

                mp3Repo.findGlobalAverageSpeed().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageAoa().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageVibrationLeft().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageVibrationRight().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageVerticalForceLeft().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageVerticalForceRight().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageLateralForceLeft().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageLateralForceRight().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageLateralVibrationLeft().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageLateralVibrationRight().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageSquareSpeed().defaultIfEmpty(0.0)
        );

        return Mono.zip(sources, results -> (Object[]) results)
                .map(arr -> {
                    // MP1 metrics
                    double mp1AvgSpeed       = cast(arr[0]);
                    double mp1AvgAoa         = cast(arr[1]);
                    double mp1AvgVibLeft     = cast(arr[2]);
                    double mp1AvgVibRight    = cast(arr[3]);
                    double mp1AvgVertFL      = cast(arr[4]);
                    double mp1AvgVertFR      = cast(arr[5]);
                    double mp1AvgLatFL       = cast(arr[6]);
                    double mp1AvgLatFR       = cast(arr[7]);
                    double mp1AvgLatVibL     = cast(arr[8]);
                    double mp1AvgLatVibR     = cast(arr[9]);
                    double mp1AvgSpeedSquare = cast(arr[10]);

                    // MP3 metrics
                    double mp3AvgSpeed       = cast(arr[11]);
                    double mp3AvgAoa         = cast(arr[12]);
                    double mp3AvgVibLeft     = cast(arr[13]);
                    double mp3AvgVibRight    = cast(arr[14]);
                    double mp3AvgVertFL      = cast(arr[15]);
                    double mp3AvgVertFR      = cast(arr[16]);
                    double mp3AvgLatFL       = cast(arr[17]);
                    double mp3AvgLatFR       = cast(arr[18]);
                    double mp3AvgLatVibL     = cast(arr[19]);
                    double mp3AvgLatVibR     = cast(arr[20]);
                    double mp3AvgSpeedSquare = cast(arr[21]);

                    // Combine linear metrics
                    double globalAvgSpeed       = combine(mp1AvgSpeed, mp3AvgSpeed);
                    double globalAvgVibLeft     = combine(mp1AvgVibLeft, mp3AvgVibLeft);
                    double globalAvgVibRight    = combine(mp1AvgVibRight, mp3AvgVibRight);
                    double globalAvgVertForceL  = combine(mp1AvgVertFL, mp3AvgVertFL);
                    double globalAvgVertForceR  = combine(mp1AvgVertFR, mp3AvgVertFR);
                    double globalAvgLatForceL   = combine(mp1AvgLatFL, mp3AvgLatFL);
                    double globalAvgLatForceR   = combine(mp1AvgLatFR, mp3AvgLatFR);
                    double globalAvgLatVibLeft  = combine(mp1AvgLatVibL, mp3AvgLatVibL);
                    double globalAvgLatVibRight = combine(mp1AvgLatVibR, mp3AvgLatVibR);

                    // Circular mean for AOA
                    double globalAvgAoa = circularMean(mp1AvgAoa, mp3AvgAoa);

                    // Variance
                    double globalAvgSpeedSquare = combine(mp1AvgSpeedSquare, mp3AvgSpeedSquare);
                    double speedVariance        = globalAvgSpeedSquare - Math.pow(globalAvgSpeed, 2);

                    // Log all of it
                    log.info("Global average speed (spd): {}", globalAvgSpeed);
                    log.info("Global average AOA   (aoa): {}", globalAvgAoa);
                    log.info("Global average vibration left (vvibl): {}", globalAvgVibLeft);
                    log.info("Global average vibration right(vvibr): {}", globalAvgVibRight);
                    log.info("Global average vertical force left (vfrcl): {}", globalAvgVertForceL);
                    log.info("Global average vertical force right(vfrcr): {}", globalAvgVertForceR);
                    log.info("Global average lateral force left (lfrcl): {}", globalAvgLatForceL);
                    log.info("Global average lateral force right(lfrcr): {}", globalAvgLatForceR);
                    log.info("Global average lateral vibration left (lvibl): {}", globalAvgLatVibLeft);
                    log.info("Global average lateral vibration right(lvibr): {}", globalAvgLatVibRight);
                    log.info("Computed global speed variance: {}", speedVariance);

                    return "";
                })
                .then()
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Scheduled(fixedRateString = "30000")
    public void scheduledAggregateSensorData() {
        aggregateSensorData()
                .subscribe(
                        unused -> log.debug("Scheduled sensor data aggregation completed."),
                        error  -> log.error("Error during scheduled aggregation", error)
                );
    }

    //––– Helpers –––//

    private Double cast(Object o) {
        return (o instanceof Double) ? (Double) o : 0.0;
    }

    private double combine(double a, double b) {
        return (a + b) / 2.0;
    }

    /**
     * Compute the circular mean of two angles (in radians):
     * handles wrap-around by averaging unit vectors.
     */
    private double circularMean(double theta1, double theta2) {
        double s = Math.sin(theta1) + Math.sin(theta2);
        double c = Math.cos(theta1) + Math.cos(theta2);
        return Math.atan2(s, c);
    }
}
