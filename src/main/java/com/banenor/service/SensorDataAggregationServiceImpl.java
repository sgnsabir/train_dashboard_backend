package com.banenor.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.dto.SensorAggregationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataAggregationServiceImpl implements SensorDataAggregationService {

    private final HaugfjellMP1AxlesRepository mp1Repo;
    private final HaugfjellMP3AxlesRepository mp3Repo;

    /**
     * Aggregates sensor data by date range.
     * @param startDate the start date in String format
     * @param endDate the end date in String format
     * @return Mono<Void> indicating the aggregation completion
     */
    @Override
    public Mono<Void> aggregateSensorDataByRange(String startDate, String endDate) {
        // Ensure that the method returns Mono<Void>
        return aggregateSensorDataByRangeReactive(startDate, endDate);
    }

    /**
     * Aggregates sensor data by date range (Reactive).
     * This method parses the input date strings into LocalDateTime and aggregates the data.
     * @param startDate the start date in String format
     * @param endDate the end date in String format
     * @return Mono<Void> with aggregated data or an empty Mono if no data.
     */
    private Mono<Void> aggregateSensorDataByRangeReactive(String startDate, String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Parse the input date strings to LocalDateTime
        LocalDateTime start = LocalDateTime.parse(startDate, formatter);
        LocalDateTime end = LocalDateTime.parse(endDate, formatter);

        // Aggregate data from both MP1 and MP3 repositories.
        Flux<SensorAggregationDTO> mp1Aggregation = mp1Repo.aggregateSensorDataByRange(start, end);
        Flux<SensorAggregationDTO> mp3Aggregation = mp3Repo.aggregateSensorDataByRange(start, end);

        // Combine the results from both repositories.
        return Flux.concat(mp1Aggregation, mp3Aggregation)
                .collectList()  // Collect results into a list
                .doOnNext(aggregatedResults -> {
                    // Log each aggregated result for monitoring and analysis.
                    aggregatedResults.forEach(result -> log.info("Aggregated Result: Vit = {}, Avg Speed = {}, Min Speed = {}, Max Speed = {}",
                            result.getVit(), result.getAvgSpeed(), result.getMinSpeed(), result.getMaxSpeed()));
                })
                .then()  // Return Mono<Void> to complete the aggregation process
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Aggregates sensor data at regular intervals (30 seconds).
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
     * Aggregates sensor data globally using reactive programming.
     */
    @Override
    public Mono<Void> aggregateSensorData() {
        return aggregateSensorDataReactive();
    }

    private Mono<Void> aggregateSensorDataReactive() {
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

        return Mono.zip(sources, results -> results)
                .map(results -> {
                    Object[] arr = (Object[]) results;
                    double globalAvgSpeed = combineAverages(cast(arr[0]), cast(arr[10]));
                    double globalAvgAoa = combineAverages(cast(arr[1]), cast(arr[11]));
                    double globalAvgVibLeft = combineAverages(cast(arr[2]), cast(arr[12]));
                    double globalAvgVibRight = combineAverages(cast(arr[3]), cast(arr[13]));
                    double globalAvgVertForceLeft = combineAverages(cast(arr[4]), cast(arr[14]));
                    double globalAvgVertForceRight = combineAverages(cast(arr[5]), cast(arr[15]));
                    double globalAvgLatForceLeft = combineAverages(cast(arr[6]), cast(arr[16]));
                    double globalAvgLatForceRight = combineAverages(cast(arr[7]), cast(arr[17]));
                    double globalAvgLatVibLeft = combineAverages(cast(arr[8]), cast(arr[18]));
                    double globalAvgLatVibRight = combineAverages(cast(arr[9]), cast(arr[19]));
                    double globalAvgSpeedSquare = combineAverages(cast(arr[10]), cast(arr[20]));
                    double speedVariance = globalAvgSpeedSquare - Math.pow(globalAvgSpeed, 2);

                    // Log the aggregated metrics
                    logAggregatedMetrics(globalAvgSpeed, globalAvgAoa, globalAvgVibLeft, globalAvgVibRight,
                            globalAvgVertForceLeft, globalAvgVertForceRight, globalAvgLatForceLeft,
                            globalAvgLatForceRight, globalAvgLatVibLeft, globalAvgLatVibRight, speedVariance);

                    return "";
                })
                .then()
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void logAggregatedMetrics(double globalAvgSpeed, double globalAvgAoa, double globalAvgVibLeft,
                                      double globalAvgVibRight, double globalAvgVertForceLeft, double globalAvgVertForceRight,
                                      double globalAvgLatForceLeft, double globalAvgLatForceRight, double globalAvgLatVibLeft,
                                      double globalAvgLatVibRight, double speedVariance) {
        log.info("Global average speed (spd): {}", globalAvgSpeed);
        log.info("Global average AOA (aoa): {}", globalAvgAoa);
        log.info("Global average vibration left (vvibl): {}", globalAvgVibLeft);
        log.info("Global average vibration right (vvibr): {}", globalAvgVibRight);
        log.info("Global average vertical force left (vfrcl): {}", globalAvgVertForceLeft);
        log.info("Global average vertical force right (vfrcr): {}", globalAvgVertForceRight);
        log.info("Global average lateral force left (lfrcl): {}", globalAvgLatForceLeft);
        log.info("Global average lateral force right (lfrcr): {}", globalAvgLatForceRight);
        log.info("Global average lateral vibration left (lvibl): {}", globalAvgLatVibLeft);
        log.info("Global average lateral vibration right (lvibr): {}", globalAvgLatVibRight);
        log.info("Computed global speed variance: {}", speedVariance);
    }

    private Double cast(Object obj) {
        return (obj != null) ? (Double) obj : 0.0;
    }

    private double combineAverages(Double val1, Double val2) {
        double first = (val1 != null) ? val1 : 0.0;
        double second = (val2 != null) ? val2 : 0.0;
        return (first + second) / 2.0;
    }
}
