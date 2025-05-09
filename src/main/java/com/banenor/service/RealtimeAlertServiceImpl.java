package com.banenor.service;

import com.banenor.alert.NotificationService;
import com.banenor.config.MaintenanceProperties;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.util.RepositoryResolver;
import com.banenor.websocket.WebSocketBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeAlertServiceImpl implements RealtimeAlertService {

    private final RepositoryResolver repositoryResolver;
    private final NotificationService notificationService;
    private final MaintenanceProperties maintenanceProperties;
    private final WebSocketBroadcaster broadcaster;

    private <T> Mono<T> safeMono(Mono<T> mono) {
        return mono != null ? mono : Mono.empty();
    }

    private final Function<Object, Double> toDouble = obj ->
            (obj instanceof Number) ? ((Number) obj).doubleValue() : 0.0;

    private Mono<Void> sendThresholdAlert(Integer trainNo,
                                          String alertEmail,
                                          String metricName,
                                          Double latestValue,
                                          Double threshold,
                                          String unit) {
        if (latestValue <= threshold) {
            return Mono.empty();
        }

        String subject = String.format("Realtime Alert: %s Exceeded", metricName);
        String message = String.format(
                "Train %d: %s = %.2f %s (threshold %.2f %s)",
                trainNo, metricName, latestValue, unit, threshold, unit
        );

        Map<String, Object> payload = Map.of(
                "trainNo", trainNo,
                "metric", metricName,
                "value", latestValue,
                "threshold", threshold,
                "unit", unit,
                "timestamp", LocalDateTime.now()
        );

        return notificationService
                .sendAlert(alertEmail, subject, message)         // Mono<Void>
                .thenEmpty(
                        Mono.fromRunnable(() -> broadcaster.publish(payload, "ALERT"))
                )                                                  // Mono<Void>
                .doOnSuccess(v -> log.debug(
                        "Alert sent & broadcast for train {} metric {}: {}",
                        trainNo, metricName, latestValue
                ))
                .onErrorResume(ex -> {
                    log.error(
                            "Failed to send/broadcast alert for train {} metric {}: {}",
                            trainNo, metricName, ex.getMessage(), ex
                    );
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> monitorAndAlert(Integer trainNo, String alertEmail) {
        return repositoryResolver.resolveRepository(trainNo)
                .flatMap(repo -> {
                    if (repo instanceof HaugfjellMP1AxlesRepository) {
                        return processAlertsForMP1((HaugfjellMP1AxlesRepository) repo, trainNo, alertEmail);
                    } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                        return processAlertsForMP3((HaugfjellMP3AxlesRepository) repo, trainNo, alertEmail);
                    }
                    return Mono.error(new IllegalArgumentException(
                            "Unsupported repository for trainNo " + trainNo
                    ));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> processAlertsForMP1(HaugfjellMP1AxlesRepository repo,
                                           Integer trainNo,
                                           String alertEmail) {
        Mono<Void> speed = safeMono(
                repo.findDynamicSpeedAggregationsByTrainNo(trainNo)
                        .map(agg -> agg.getAvgSpeed())
                        .reduce(Double::max)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> sendThresholdAlert(
                trainNo, alertEmail, "Speed", toDouble.apply(latest),
                maintenanceProperties.getRealtimeSpeedThreshold(), "km/h"
        ));

        Mono<Void> vibLeft = safeMono(
                repo.findDynamicVibrationLeftAggregationsByTrainNo(trainNo)
                        .map(agg -> agg.getAvgVibrationLeft())
                        .reduce(Double::max)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> sendThresholdAlert(
                trainNo, alertEmail, "Left Vibration", toDouble.apply(latest),
                maintenanceProperties.getRealtimeVibrationLeftThreshold(), ""
        ));

        Mono<Void> vibRight = safeMono(
                repo.findDynamicVibrationRightAggregationsByTrainNo(trainNo)
                        .map(agg -> agg.getAvgVibrationRight())
                        .reduce(Double::max)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> sendThresholdAlert(
                trainNo, alertEmail, "Right Vibration", toDouble.apply(latest),
                maintenanceProperties.getRealtimeVibrationRightThreshold(), ""
        ));

        Mono<Void> vfLeft = safeMono(
                repo.findDynamicVerticalForceLeftAggregationsByTrainNo(trainNo)
                        .map(agg -> agg.getAvgVerticalForceLeft())
                        .reduce(Double::max)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> sendThresholdAlert(
                trainNo, alertEmail, "Vertical Force Left", toDouble.apply(latest),
                maintenanceProperties.getRealtimeVerticalForceLeftThreshold(), "kN"
        ));

        Mono<Void> vfRight = safeMono(
                repo.findDynamicVerticalForceRightAggregationsByTrainNo(trainNo)
                        .map(agg -> agg.getAvgVerticalForceRight())
                        .reduce(Double::max)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> sendThresholdAlert(
                trainNo, alertEmail, "Vertical Force Right", toDouble.apply(latest),
                maintenanceProperties.getRealtimeVerticalForceRightThreshold(), "kN"
        ));

        Mono<Void> lfRight = safeMono(
                repo.findDynamicLateralForceRightAggregationsByTrainNo(trainNo)
                        .map(agg -> agg.getAvgLateralForceRight())
                        .reduce(Double::max)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> sendThresholdAlert(
                trainNo, alertEmail, "Lateral Force Right", toDouble.apply(latest),
                maintenanceProperties.getRealtimeLateralForceRightThreshold(), "kN"
        ));

        Mono<Void> lvRight = safeMono(
                repo.findDynamicLateralVibrationRightAggregationsByTrainNo(trainNo)
                        .map(agg -> agg.getAvgLateralVibrationRight())
                        .reduce(Double::max)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> sendThresholdAlert(
                trainNo, alertEmail, "Lateral Vibration Right", toDouble.apply(latest),
                maintenanceProperties.getRealtimeLateralVibrationRightThreshold(), ""
        ));

        return Mono.whenDelayError(speed, vibLeft, vibRight, vfLeft, vfRight, lfRight, lvRight);
    }

    private Mono<Void> processAlertsForMP3(HaugfjellMP3AxlesRepository repo,
                                           Integer trainNo,
                                           String alertEmail) {
        Mono<Void> speed = safeMono(
                repo.findDynamicSpeedAggregationsByTrainNo(trainNo)
                        .map(agg -> agg.getAvgSpeed())
                        .reduce(Double::max)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> sendThresholdAlert(
                trainNo, alertEmail, "Speed", toDouble.apply(latest),
                maintenanceProperties.getRealtimeSpeedThreshold(), "km/h"
        ));

        Mono<Void> vibLeft = safeMono(
                repo.findDynamicVibrationLeftAggregationsByTrainNo(trainNo)
                        .map(agg -> agg.getAvgVibrationLeft())
                        .reduce(Double::max)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> sendThresholdAlert(
                trainNo, alertEmail, "Left Vibration", toDouble.apply(latest),
                maintenanceProperties.getRealtimeVibrationLeftThreshold(), ""
        ));

        Mono<Void> vibRight = safeMono(
                repo.findDynamicVibrationRightAggregationsByTrainNo(trainNo)
                        .map(agg -> agg.getAvgVibrationRight())
                        .reduce(Double::max)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> sendThresholdAlert(
                trainNo, alertEmail, "Right Vibration", toDouble.apply(latest),
                maintenanceProperties.getRealtimeVibrationRightThreshold(), ""
        ));

        Mono<Void> vfLeft = safeMono(
                repo.findDynamicVerticalForceLeftAggregationsByTrainNo(trainNo)
                        .map(agg -> agg.getAvgVerticalForceLeft())
                        .reduce(Double::max)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> sendThresholdAlert(
                trainNo, alertEmail, "Vertical Force Left", toDouble.apply(latest),
                maintenanceProperties.getRealtimeVerticalForceLeftThreshold(), "kN"
        ));

        Mono<Void> vfRight = safeMono(
                repo.findDynamicVerticalForceRightAggregationsByTrainNo(trainNo)
                        .map(agg -> agg.getAvgVerticalForceRight())
                        .reduce(Double::max)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> sendThresholdAlert(
                trainNo, alertEmail, "Vertical Force Right", toDouble.apply(latest),
                maintenanceProperties.getRealtimeVerticalForceRightThreshold(), "kN"
        ));

        Mono<Void> lfRight = safeMono(
                repo.findDynamicLateralForceRightAggregationsByTrainNo(trainNo)
                        .map(agg -> agg.getAvgLateralForceRight())
                        .reduce(Double::max)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> sendThresholdAlert(
                trainNo, alertEmail, "Lateral Force Right", toDouble.apply(latest),
                maintenanceProperties.getRealtimeLateralForceRightThreshold(), "kN"
        ));

        Mono<Void> lvRight = safeMono(
                repo.findDynamicLateralVibrationRightAggregationsByTrainNo(trainNo)
                        .map(agg -> agg.getAvgLateralVibrationRight())
                        .reduce(Double::max)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> sendThresholdAlert(
                trainNo, alertEmail, "Lateral Vibration Right", toDouble.apply(latest),
                maintenanceProperties.getRealtimeLateralVibrationRightThreshold(), ""
        ));

        return Mono.whenDelayError(speed, vibLeft, vibRight, vfLeft, vfRight, lfRight, lvRight);
    }
}
