package com.banenor.service;

import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.util.RepositoryResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationServiceImpl implements AggregationService {

    private final RepositoryResolver repositoryResolver;
    private final HaugfjellMP1AxlesRepository mp1Repo;
    private final HaugfjellMP3AxlesRepository mp3Repo;
    private final CacheService cacheService;

    //───────────────────────────────────────────────────────────────────────────────
    // COMBINED VIBRATION METRIC (for AdminDashboard "avgVibration")
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Double> getAverageVibration(Integer trainNo) {
        return Mono.zip(
                        getAverageVibrationLeft(trainNo),
                        getAverageVibrationRight(trainNo),
                        (left, right) -> (left + right) / 2.0
                )
                .doOnNext(avg -> {
                    log.debug("Computed combined average vibration for train {} = {}", trainNo, avg);
                    cacheService.cacheAverage("avgVibration", avg).subscribe();
                })
                .onErrorResume(e -> {
                    log.warn("Error computing combined average vibration for train {}: {}", trainNo, e.getMessage());
                    return Mono.just(0.0);
                });
    }

    //───────────────────────────────────────────────────────────────────────────────
    // SPEED METRICS
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Double> getAverageSpeed(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgSpeedByTrainNo(trainNo),
                repo -> repo.findOverallAvgSpeedByTrainNo(trainNo),
                "average speed"
        )
                .doOnNext(avg -> {
                    log.debug("Caching avgSpeed for train {} = {}", trainNo, avg);
                    cacheService.cacheAverage("avgSpeed", avg).subscribe();
                });
    }

    @Override
    public Mono<Double> getMinSpeed(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMinSpeedByTrainNo(trainNo),
                repo -> repo.findOverallMinSpeedByTrainNo(trainNo),
                "min speed"
        );
    }

    @Override
    public Mono<Double> getMaxSpeed(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMaxSpeedByTrainNo(trainNo),
                repo -> repo.findOverallMaxSpeedByTrainNo(trainNo),
                "max speed"
        );
    }

    @Override
    public Mono<Double> getSpeedVariance(Integer trainNo) {
        return Mono.zip(
                        getAverageSquareSpeed(trainNo),
                        getAverageSpeed(trainNo),
                        (avgSq, avg) -> avgSq - (avg * avg)
                )
                .doOnSuccess(v -> log.debug("Speed variance for train {} = {}", trainNo, v))
                .onErrorResume(e -> {
                    log.warn("Error computing speed variance for train {}: {}", trainNo, e.getMessage());
                    return Mono.just(0.0);
                });
    }

    private Mono<Double> getAverageSquareSpeed(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgSquareSpeedByTrainNo(trainNo),
                repo -> repo.findOverallAvgSquareSpeedByTrainNo(trainNo),
                "average square speed"
        );
    }

    //───────────────────────────────────────────────────────────────────────────────
    // ANGLE-OF-ATTACK METRICS
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Double> getAverageAoa(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgAoaByTrainNo(trainNo),
                repo -> repo.findOverallAvgAoaByTrainNo(trainNo),
                "average AOA"
        )
                .doOnNext(avg -> {
                    log.debug("Caching avgAoa for train {} = {}", trainNo, avg);
                    cacheService.cacheAverage("avgAoa", avg).subscribe();
                });
    }

    @Override
    public Mono<Double> getMinAoa(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMinAoaByTrainNo(trainNo),
                repo -> repo.findOverallMinAoaByTrainNo(trainNo),
                "min AOA"
        );
    }

    @Override
    public Mono<Double> getMaxAoa(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMaxAoaByTrainNo(trainNo),
                repo -> repo.findOverallMaxAoaByTrainNo(trainNo),
                "max AOA"
        );
    }

    @Override
    public Mono<Double> getAoaVariance(Integer trainNo) {
        return Mono.zip(
                        getAverageSquareAoa(trainNo),
                        getAverageAoa(trainNo),
                        (avgSq, avg) -> avgSq - (avg * avg)
                )
                .doOnSuccess(v -> log.debug("AOA variance for train {} = {}", trainNo, v))
                .onErrorResume(e -> {
                    log.warn("Error computing AOA variance for train {}: {}", trainNo, e.getMessage());
                    return Mono.just(0.0);
                });
    }

    private Mono<Double> getAverageSquareAoa(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgSquareAoaByTrainNo(trainNo),
                repo -> repo.findOverallAvgSquareAoaByTrainNo(trainNo),
                "average square AOA"
        );
    }

    //───────────────────────────────────────────────────────────────────────────────
    // VIBRATION (LEFT) METRICS
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Double> getAverageVibrationLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgVibrationLeftByTrainNo(trainNo),
                repo -> repo.findOverallAvgVibrationLeftByTrainNo(trainNo),
                "average vibration left"
        )
                .doOnNext(avg -> {
                    log.debug("Caching avgVibrationLeft for train {} = {}", trainNo, avg);
                    cacheService.cacheAverage("avgVibrationLeft", avg).subscribe();
                });
    }

    @Override
    public Mono<Double> getMinVibrationLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMinVibrationLeftByTrainNo(trainNo),
                repo -> repo.findOverallMinVibrationLeftByTrainNo(trainNo),
                "min vibration left"
        );
    }

    @Override
    public Mono<Double> getMaxVibrationLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMaxVibrationLeftByTrainNo(trainNo),
                repo -> repo.findOverallMaxVibrationLeftByTrainNo(trainNo),
                "max vibration left"
        );
    }

    @Override
    public Mono<Double> getVibrationLeftVariance(Integer trainNo) {
        return Mono.zip(
                        getAverageSquareVibrationLeft(trainNo),
                        getAverageVibrationLeft(trainNo),
                        (avgSq, avg) -> avgSq - (avg * avg)
                )
                .doOnSuccess(v -> log.debug("Vibration-left variance for train {} = {}", trainNo, v))
                .onErrorResume(e -> {
                    log.warn("Error computing vibration-left variance for train {}: {}", trainNo, e.getMessage());
                    return Mono.just(0.0);
                });
    }

    private Mono<Double> getAverageSquareVibrationLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgSquareVibrationLeftByTrainNo(trainNo),
                repo -> repo.findOverallAvgSquareVibrationLeftByTrainNo(trainNo),
                "average square vibration left"
        );
    }

    //───────────────────────────────────────────────────────────────────────────────
    // VIBRATION (RIGHT) METRICS
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Double> getAverageVibrationRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgVibrationRightByTrainNo(trainNo),
                repo -> repo.findOverallAvgVibrationRightByTrainNo(trainNo),
                "average vibration right"
        )
                .doOnNext(avg -> {
                    log.debug("Caching avgVibrationRight for train {} = {}", trainNo, avg);
                    cacheService.cacheAverage("avgVibrationRight", avg).subscribe();
                });
    }

    @Override
    public Mono<Double> getMinVibrationRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMinVibrationRightByTrainNo(trainNo),
                repo -> repo.findOverallMinVibrationRightByTrainNo(trainNo),
                "min vibration right"
        );
    }

    @Override
    public Mono<Double> getMaxVibrationRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMaxVibrationRightByTrainNo(trainNo),
                repo -> repo.findOverallMaxVibrationRightByTrainNo(trainNo),
                "max vibration right"
        );
    }

    @Override
    public Mono<Double> getVibrationRightVariance(Integer trainNo) {
        return Mono.zip(
                        getAverageSquareVibrationRight(trainNo),
                        getAverageVibrationRight(trainNo),
                        (avgSq, avg) -> avgSq - (avg * avg)
                )
                .doOnSuccess(v -> log.debug("Vibration-right variance for train {} = {}", trainNo, v))
                .onErrorResume(e -> {
                    log.warn("Error computing vibration-right variance for train {}: {}", trainNo, e.getMessage());
                    return Mono.just(0.0);
                });
    }

    private Mono<Double> getAverageSquareVibrationRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgSquareVibrationRightByTrainNo(trainNo),
                repo -> repo.findOverallAvgSquareVibrationRightByTrainNo(trainNo),
                "average square vibration right"
        );
    }

    //───────────────────────────────────────────────────────────────────────────────
    // VERTICAL FORCE (LEFT) METRICS
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Double> getAverageVerticalForceLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgVerticalForceLeftByTrainNo(trainNo),
                repo -> repo.findOverallAvgVerticalForceLeftByTrainNo(trainNo),
                "average vertical force left"
        )
                .doOnNext(avg -> {
                    log.debug("Caching avgVerticalForceLeft for train {} = {}", trainNo, avg);
                    cacheService.cacheAverage("avgVerticalForceLeft", avg).subscribe();
                });
    }

    @Override
    public Mono<Double> getMinVerticalForceLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMinVerticalForceLeftByTrainNo(trainNo),
                repo -> repo.findOverallMinVerticalForceLeftByTrainNo(trainNo),
                "min vertical force left"
        );
    }

    @Override
    public Mono<Double> getMaxVerticalForceLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMaxVerticalForceLeftByTrainNo(trainNo),
                repo -> repo.findOverallMaxVerticalForceLeftByTrainNo(trainNo),
                "max vertical force left"
        );
    }

    @Override
    public Mono<Double> getVerticalForceLeftVariance(Integer trainNo) {
        return Mono.zip(
                        getAverageSquareVerticalForceLeft(trainNo),
                        getAverageVerticalForceLeft(trainNo),
                        (avgSq, avg) -> avgSq - (avg * avg)
                )
                .doOnSuccess(v -> log.debug("Vertical-force-left variance for train {} = {}", trainNo, v))
                .onErrorResume(e -> {
                    log.warn("Error computing vertical-force-left variance for train {}: {}", trainNo, e.getMessage());
                    return Mono.just(0.0);
                });
    }

    private Mono<Double> getAverageSquareVerticalForceLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgSquareVerticalForceLeftByTrainNo(trainNo),
                repo -> repo.findOverallAvgSquareVerticalForceLeftByTrainNo(trainNo),
                "average square vertical force left"
        );
    }

    //───────────────────────────────────────────────────────────────────────────────
    // VERTICAL FORCE (RIGHT) METRICS
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Double> getAverageVerticalForceRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgVerticalForceRightByTrainNo(trainNo),
                repo -> repo.findOverallAvgVerticalForceRightByTrainNo(trainNo),
                "average vertical force right"
        )
                .doOnNext(avg -> {
                    log.debug("Caching avgVerticalForceRight for train {} = {}", trainNo, avg);
                    cacheService.cacheAverage("avgVerticalForceRight", avg).subscribe();
                });
    }

    @Override
    public Mono<Double> getMinVerticalForceRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMinVerticalForceRightByTrainNo(trainNo),
                repo -> repo.findOverallMinVerticalForceRightByTrainNo(trainNo),
                "min vertical force right"
        );
    }

    @Override
    public Mono<Double> getMaxVerticalForceRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMaxVerticalForceRightByTrainNo(trainNo),
                repo -> repo.findOverallMaxVerticalForceRightByTrainNo(trainNo),
                "max vertical force right"
        );
    }

    @Override
    public Mono<Double> getVerticalForceRightVariance(Integer trainNo) {
        return Mono.zip(
                        getAverageSquareVerticalForceRight(trainNo),
                        getAverageVerticalForceRight(trainNo),
                        (avgSq, avg) -> avgSq - (avg * avg)
                )
                .doOnSuccess(v -> log.debug("Vertical-force-right variance for train {} = {}", trainNo, v))
                .onErrorResume(e -> {
                    log.warn("Error computing vertical-force-right variance for train {}: {}", trainNo, e.getMessage());
                    return Mono.just(0.0);
                });
    }

    private Mono<Double> getAverageSquareVerticalForceRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgSquareVerticalForceRightByTrainNo(trainNo),
                repo -> repo.findOverallAvgSquareVerticalForceRightByTrainNo(trainNo),
                "average square vertical force right"
        );
    }

    //───────────────────────────────────────────────────────────────────────────────
    // LATERAL FORCE (LEFT) METRICS
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Double> getAverageLateralForceLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgLateralForceLeftByTrainNo(trainNo),
                repo -> repo.findOverallAvgLateralForceLeftByTrainNo(trainNo),
                "average lateral force left"
        )
                .doOnNext(avg -> {
                    log.debug("Caching avgLateralForceLeft for train {} = {}", trainNo, avg);
                    cacheService.cacheAverage("avgLateralForceLeft", avg).subscribe();
                });
    }

    @Override
    public Mono<Double> getMinLateralForceLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMinLateralForceLeftByTrainNo(trainNo),
                repo -> repo.findOverallMinLateralForceLeftByTrainNo(trainNo),
                "min lateral force left"
        );
    }

    @Override
    public Mono<Double> getMaxLateralForceLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMaxLateralForceLeftByTrainNo(trainNo),
                repo -> repo.findOverallMaxLateralForceLeftByTrainNo(trainNo),
                "max lateral force left"
        );
    }

    @Override
    public Mono<Double> getLateralForceLeftVariance(Integer trainNo) {
        return Mono.zip(
                        getAverageSquareLateralForceLeft(trainNo),
                        getAverageLateralForceLeft(trainNo),
                        (avgSq, avg) -> avgSq - (avg * avg)
                )
                .doOnSuccess(v -> log.debug("Lateral-force-left variance for train {} = {}", trainNo, v))
                .onErrorResume(e -> {
                    log.warn("Error computing lateral-force-left variance for train {}: {}", trainNo, e.getMessage());
                    return Mono.just(0.0);
                });
    }

    private Mono<Double> getAverageSquareLateralForceLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgSquareLateralForceLeftByTrainNo(trainNo),
                repo -> repo.findOverallAvgSquareLateralForceLeftByTrainNo(trainNo),
                "average square lateral force left"
        );
    }

    //───────────────────────────────────────────────────────────────────────────────
    // LATERAL FORCE (RIGHT) METRICS
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Double> getAverageLateralForceRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgLateralForceRightByTrainNo(trainNo),
                repo -> repo.findOverallAvgLateralForceRightByTrainNo(trainNo),
                "average lateral force right"
        )
                .doOnNext(avg -> {
                    log.debug("Caching avgLateralForceRight for train {} = {}", trainNo, avg);
                    cacheService.cacheAverage("avgLateralForceRight", avg).subscribe();
                });
    }

    @Override
    public Mono<Double> getMinLateralForceRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMinLateralForceRightByTrainNo(trainNo),
                repo -> repo.findOverallMinLateralForceRightByTrainNo(trainNo),
                "min lateral force right"
        );
    }

    @Override
    public Mono<Double> getMaxLateralForceRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMaxLateralForceRightByTrainNo(trainNo),
                repo -> repo.findOverallMaxLateralForceRightByTrainNo(trainNo),
                "max lateral force right"
        );
    }

    @Override
    public Mono<Double> getLateralForceRightVariance(Integer trainNo) {
        return Mono.zip(
                        getAverageSquareLateralForceRight(trainNo),
                        getAverageLateralForceRight(trainNo),
                        (avgSq, avg) -> avgSq - (avg * avg)
                )
                .doOnSuccess(v -> log.debug("Lateral-force-right variance for train {} = {}", trainNo, v))
                .onErrorResume(e -> {
                    log.warn("Error computing lateral-force-right variance for train {}: {}", trainNo, e.getMessage());
                    return Mono.just(0.0);
                });
    }

    private Mono<Double> getAverageSquareLateralForceRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgSquareLateralForceRightByTrainNo(trainNo),
                repo -> repo.findOverallAvgSquareLateralForceRightByTrainNo(trainNo),
                "average square lateral force right"
        );
    }

    //───────────────────────────────────────────────────────────────────────────────
    // LATERAL VIBRATION (LEFT) METRICS
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Double> getAverageLateralVibrationLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgLateralVibrationLeftByTrainNo(trainNo),
                repo -> repo.findOverallAvgLateralVibrationLeftByTrainNo(trainNo),
                "average lateral vibration left"
        )
                .doOnNext(avg -> {
                    log.debug("Caching avgLateralVibrationLeft for train {} = {}", trainNo, avg);
                    cacheService.cacheAverage("avgLateralVibrationLeft", avg).subscribe();
                });
    }

    @Override
    public Mono<Double> getMinLateralVibrationLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMinLateralVibrationLeftByTrainNo(trainNo),
                repo -> repo.findOverallMinLateralVibrationLeftByTrainNo(trainNo),
                "min lateral vibration left"
        );
    }

    @Override
    public Mono<Double> getMaxLateralVibrationLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMaxLateralVibrationLeftByTrainNo(trainNo),
                repo -> repo.findOverallMaxLateralVibrationLeftByTrainNo(trainNo),
                "max lateral vibration left"
        );
    }

    @Override
    public Mono<Double> getLateralVibrationLeftVariance(Integer trainNo) {
        return Mono.zip(
                        getAverageSquareLateralVibrationLeft(trainNo),
                        getAverageLateralVibrationLeft(trainNo),
                        (avgSq, avg) -> avgSq - (avg * avg)
                )
                .doOnSuccess(v -> log.debug("Lateral-vibration-left variance for train {} = {}", trainNo, v))
                .onErrorResume(e -> {
                    log.warn("Error computing lateral-vibration-left variance for train {}: {}", trainNo, e.getMessage());
                    return Mono.just(0.0);
                });
    }

    private Mono<Double> getAverageSquareLateralVibrationLeft(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgSquareLateralVibrationLeftByTrainNo(trainNo),
                repo -> repo.findOverallAvgSquareLateralVibrationLeftByTrainNo(trainNo),
                "average square lateral vibration left"
        );
    }

    //───────────────────────────────────────────────────────────────────────────────
    // LATERAL VIBRATION (RIGHT) METRICS
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Double> getAverageLateralVibrationRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgLateralVibrationRightByTrainNo(trainNo),
                repo -> repo.findOverallAvgLateralVibrationRightByTrainNo(trainNo),
                "average lateral vibration right"
        )
                .doOnNext(avg -> {
                    log.debug("Caching avgLateralVibrationRight for train {} = {}", trainNo, avg);
                    cacheService.cacheAverage("avgLateralVibrationRight", avg).subscribe();
                });
    }

    @Override
    public Mono<Double> getMinLateralVibrationRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMinLateralVibrationRightByTrainNo(trainNo),
                repo -> repo.findOverallMinLateralVibrationRightByTrainNo(trainNo),
                "min lateral vibration right"
        );
    }

    @Override
    public Mono<Double> getMaxLateralVibrationRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallMaxLateralVibrationRightByTrainNo(trainNo),
                repo -> repo.findOverallMaxLateralVibrationRightByTrainNo(trainNo),
                "max lateral vibration right"
        );
    }

    @Override
    public Mono<Double> getLateralVibrationRightVariance(Integer trainNo) {
        return Mono.zip(
                        getAverageSquareLateralVibrationRight(trainNo),
                        getAverageLateralVibrationRight(trainNo),
                        (avgSq, avg) -> avgSq - (avg * avg)
                )
                .doOnSuccess(v -> log.debug("Lateral-vibration-right variance for train {} = {}", trainNo, v))
                .onErrorResume(e -> {
                    log.warn("Error computing lateral-vibration-right variance for train {}: {}", trainNo, e.getMessage());
                    return Mono.just(0.0);
                });
    }

    private Mono<Double> getAverageSquareLateralVibrationRight(Integer trainNo) {
        return queryMetric(
                trainNo,
                repo -> repo.findOverallAvgSquareLateralVibrationRightByTrainNo(trainNo),
                repo -> repo.findOverallAvgSquareLateralVibrationRightByTrainNo(trainNo),
                "average square lateral vibration right"
        );
    }

    //───────────────────────────────────────────────────────────────────────────────
    // GENERIC ROUTING HELPER
    //───────────────────────────────────────────────────────────────────────────────

    /**
     * Routes to the correct repository implementation,
     * applies the given function and handles errors uniformly.
     */
    private Mono<Double> queryMetric(
            Integer trainNo,
            Function<HaugfjellMP1AxlesRepository, Mono<Double>> mp1Query,
            Function<HaugfjellMP3AxlesRepository, Mono<Double>> mp3Query,
            String metricName
    ) {
        Objects.requireNonNull(trainNo, metricName + " requires a train number");

        return repositoryResolver.resolveRepository(trainNo)
                .flatMap(repo -> {
                    if (repo instanceof HaugfjellMP1AxlesRepository r1) {
                        return mp1Query.apply(r1);
                    } else if (repo instanceof HaugfjellMP3AxlesRepository r3) {
                        return mp3Query.apply(r3);
                    } else {
                        return Mono.error(new IllegalStateException("No matching repository for train " + trainNo));
                    }
                })
                .doOnSuccess(v -> log.debug("{} for train {} = {}", metricName, trainNo, v))
                .doOnError(e -> log.warn("Error fetching {} for train {}: {}", metricName, trainNo, e.getMessage()))
                .onErrorReturn(0.0);
    }
}
