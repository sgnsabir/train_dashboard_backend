package com.banenor.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataAggregationServiceImpl implements SensorDataAggregationService {

    private final HaugfjellMP1AxlesRepository mp1Repo;
    private final HaugfjellMP3AxlesRepository mp3Repo;

    /**
     * Aggregates sensor data globally from both MP1 and MP3 repositories.
     * This method returns a Mono that completes when aggregation and logging is done.
     */
    @Override
    public Mono<Void> aggregateSensorData() {
        return aggregateSensorDataReactive();
    }

    /**
     * Scheduled method to trigger sensor data aggregation every 30 seconds.
     */
    @Scheduled(fixedRateString = "30000")
    public void scheduledAggregateSensorData() {
        aggregateSensorData()
                .subscribe(
                        unused -> log.debug("Scheduled sensor data aggregation completed successfully."),
                        error -> log.error("Error during scheduled sensor data aggregation", error)
                );
    }

    /**
     * Composes the aggregation logic using reactive streams.
     *
     * @return a Mono that completes when aggregation is finished.
     */
    private Mono<Void> aggregateSensorDataReactive() {
        // Define a list of Monos to aggregate different global metrics.
        List<Mono<Double>> sources = List.of(
                mp1Repo.findGlobalAverageSpeed().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageSpeed().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageAoa().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageAoa().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageVibrationLeft().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageVibrationLeft().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageVibrationRight().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageVibrationRight().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageVerticalForceLeft().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageVerticalForceLeft().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageVerticalForceRight().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageVerticalForceRight().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageLateralForceLeft().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageLateralForceLeft().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageLateralForceRight().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageLateralForceRight().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageLateralVibrationLeft().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageLateralVibrationLeft().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageLateralVibrationRight().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageLateralVibrationRight().defaultIfEmpty(0.0),
                mp1Repo.findGlobalAverageSquareSpeed().defaultIfEmpty(0.0),
                mp3Repo.findGlobalAverageSquareSpeed().defaultIfEmpty(0.0)
        );

        return Mono.zip(sources, results -> results)
                .map(results -> {
                    Object[] arr = (Object[]) results;
                    double globalAvgSpeed = combineAverages(cast(arr[0]), cast(arr[1]));
                    double globalAvgAoa = combineAverages(cast(arr[2]), cast(arr[3]));
                    double globalAvgVibLeft = combineAverages(cast(arr[4]), cast(arr[5]));
                    double globalAvgVibRight = combineAverages(cast(arr[6]), cast(arr[7]));
                    double globalAvgVertForceLeft = combineAverages(cast(arr[8]), cast(arr[9]));
                    double globalAvgVertForceRight = combineAverages(cast(arr[10]), cast(arr[11]));
                    double globalAvgLatForceLeft = combineAverages(cast(arr[12]), cast(arr[13]));
                    double globalAvgLatForceRight = combineAverages(cast(arr[14]), cast(arr[15]));
                    double globalAvgLatVibLeft = combineAverages(cast(arr[16]), cast(arr[17]));
                    double globalAvgLatVibRight = combineAverages(cast(arr[18]), cast(arr[19]));
                    double globalAvgSpeedSquare = combineAverages(cast(arr[20]), cast(arr[21]));
                    double speedVariance = globalAvgSpeedSquare - Math.pow(globalAvgSpeed, 2);

                    log.info("Global average speed (spd_tp1): {}", globalAvgSpeed);
                    log.info("Global average AOA (aoa_tp1): {}", globalAvgAoa);
                    log.info("Global average vibration left (vvibl_tp1): {}", globalAvgVibLeft);
                    log.info("Global average vibration right (vvibr_tp1): {}", globalAvgVibRight);
                    log.info("Global average vertical force left (vfrcl_tp1): {}", globalAvgVertForceLeft);
                    log.info("Global average vertical force right (vfrcr_tp1): {}", globalAvgVertForceRight);
                    log.info("Global average lateral force left (lfrcl_tp1): {}", globalAvgLatForceLeft);
                    log.info("Global average lateral force right (lfrcr_tp1): {}", globalAvgLatForceRight);
                    log.info("Global average lateral vibration left (lvibl_tp1): {}", globalAvgLatVibLeft);
                    log.info("Global average lateral vibration right (lvibr_tp1): {}", globalAvgLatVibRight);
                    log.info("Computed global speed variance: {}", speedVariance);

                    // Return a non-null marker to satisfy the zip function.
                    return "";
                })
                .then()
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Helper method to cast an Object to a Double.
     *
     * @param obj the object to cast
     * @return the Double value or 0.0 if the object is null
     */
    private Double cast(Object obj) {
        return (obj != null) ? (Double) obj : 0.0;
    }

    /**
     * Helper method to compute the average of two Double values.
     *
     * @param val1 the first value
     * @param val2 the second value
     * @return the computed average
     */
    private double combineAverages(Double val1, Double val2) {
        double first = (val1 != null) ? val1 : 0.0;
        double second = (val2 != null) ? val2 : 0.0;
        return (first + second) / 2.0;
    }
}
