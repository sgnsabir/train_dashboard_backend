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
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeAlertServiceImpl implements RealtimeAlertService {

    private final RepositoryResolver repositoryResolver;
    private final NotificationService notificationService;
    private final MaintenanceProperties maintenanceProperties;
    private final WebSocketBroadcaster broadcaster;

    private static final Function<Object, Double> toDouble = obj ->
            (obj instanceof Number) ? ((Number) obj).doubleValue() : 0.0;

    // --------------------------
    // PUBLIC ENTRY POINT
    // --------------------------
    @Override
    public Mono<Void> monitorAndAlert(Integer trainNo,
                                      String alertEmail,
                                      String subject,
                                      String message) {
        boolean isCustom = Optional.ofNullable(subject).filter(s -> !s.isBlank()).isPresent()
                && Optional.ofNullable(message).filter(m -> !m.isBlank()).isPresent();

        if (isCustom) {
            log.debug("Processing manual alert for train {} with subject='{}'", trainNo, subject);
            return sendCustomAlert(trainNo, alertEmail, subject, message)
                    .subscribeOn(Schedulers.boundedElastic());
        }

        log.debug("Processing threshold alerts for train {}", trainNo);
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

    // --------------------------
    // MANUAL ALERT
    // --------------------------
    private Mono<Void> sendCustomAlert(Integer trainNo,
                                       String alertEmail,
                                       String subject,
                                       String message) {
        Map<String,Object> payload = Map.of(
                "trainNo",   trainNo,
                "subject",   subject,
                "message",   message,
                "timestamp", LocalDateTime.now()
        );

        return notificationService
                .sendAlert(alertEmail, subject, message)
                .then(Mono.fromRunnable(() -> broadcaster.publish(payload, "ALERT")))
                .doOnSuccess(v -> log.info("Manual alert sent for train {}", trainNo))
                .onErrorResume(ex -> {
                    log.error("Failed manual alert for train {}: {}", trainNo, ex.getMessage(), ex);
                    return Mono.empty();
                }).then();
    }

    // --------------------------
    // THRESHOLD ALERT MP1
    // --------------------------
    private Mono<Void> processAlertsForMP1(HaugfjellMP1AxlesRepository repo,
                                           Integer trainNo,
                                           String alertEmail) {
        Mono<Void> speed = repo.findDynamicSpeedAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgSpeed())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Speed", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeSpeedThreshold(), "km/h"
                ));

        Mono<Void> vibLeft = repo.findDynamicVibrationLeftAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgVibrationLeft())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Left Vibration", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeVibrationLeftThreshold(), ""
                ));

        Mono<Void> vibRight = repo.findDynamicVibrationRightAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgVibrationRight())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Right Vibration", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeVibrationRightThreshold(), ""
                ));

        Mono<Void> vfLeft = repo.findDynamicVerticalForceLeftAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgVerticalForceLeft())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Vertical Force Left", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeVerticalForceLeftThreshold(), "kN"
                ));

        Mono<Void> vfRight = repo.findDynamicVerticalForceRightAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgVerticalForceRight())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Vertical Force Right", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeVerticalForceRightThreshold(), "kN"
                ));

        Mono<Void> lfLeft = repo.findDynamicLateralForceLeftAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgLateralForceLeft())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Lateral Force Left", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeLateralForceLeftThreshold(), "kN"
                ));

        Mono<Void> lfRight = repo.findDynamicLateralForceRightAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgLateralForceRight())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Lateral Force Right", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeLateralForceRightThreshold(), "kN"
                ));

        Mono<Void> lvLeft = repo.findDynamicLateralVibrationLeftAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgLateralVibrationLeft())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Lateral Vibration Left", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeLateralVibrationLeftThreshold(), ""
                ));

        Mono<Void> lvRight = repo.findDynamicLateralVibrationRightAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgLateralVibrationRight())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Lateral Vibration Right", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeLateralVibrationRightThreshold(), ""
                ));

        return Mono.whenDelayError(
                speed, vibLeft, vibRight,
                vfLeft, vfRight, lfLeft,
                lfRight, lvLeft, lvRight
        );
    }

    // --------------------------
    // THRESHOLD ALERT MP3
    // --------------------------
    private Mono<Void> processAlertsForMP3(HaugfjellMP3AxlesRepository repo,
                                           Integer trainNo,
                                           String alertEmail) {
        // Duplicate MP1 logic but against MP3 repository
        Mono<Void> speed = repo.findDynamicSpeedAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgSpeed())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Speed", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeSpeedThreshold(), "km/h"
                ));

        Mono<Void> vibLeft = repo.findDynamicVibrationLeftAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgVibrationLeft())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Left Vibration", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeVibrationLeftThreshold(), ""
                ));

        Mono<Void> vibRight = repo.findDynamicVibrationRightAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgVibrationRight())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Right Vibration", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeVibrationRightThreshold(), ""
                ));

        Mono<Void> vfLeft = repo.findDynamicVerticalForceLeftAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgVerticalForceLeft())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Vertical Force Left", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeVerticalForceLeftThreshold(), "kN"
                ));

        Mono<Void> vfRight = repo.findDynamicVerticalForceRightAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgVerticalForceRight())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Vertical Force Right", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeVerticalForceRightThreshold(), "kN"
                ));

        Mono<Void> lfLeft = repo.findDynamicLateralForceLeftAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgLateralForceLeft())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Lateral Force Left", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeLateralForceLeftThreshold(), "kN"
                ));

        Mono<Void> lfRight = repo.findDynamicLateralForceRightAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgLateralForceRight())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Lateral Force Right", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeLateralForceRightThreshold(), "kN"
                ));

        Mono<Void> lvLeft = repo.findDynamicLateralVibrationLeftAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgLateralVibrationLeft())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Lateral Vibration Left", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeLateralVibrationLeftThreshold(), ""
                ));

        Mono<Void> lvRight = repo.findDynamicLateralVibrationRightAggregationsByTrainNo(trainNo)
                .map(agg -> agg.getAvgLateralVibrationRight())
                .reduce(Double::max)
                .defaultIfEmpty(0.0)
                .flatMap(latest -> sendThresholdAlert(
                        trainNo, alertEmail, "Lateral Vibration Right", toDouble.apply(latest),
                        maintenanceProperties.getRealtimeLateralVibrationRightThreshold(), ""
                ));

        return Mono.whenDelayError(
                speed, vibLeft, vibRight,
                vfLeft, vfRight, lfLeft,
                lfRight, lvLeft, lvRight
        );
    }

    // --------------------------
    // COMMON THRESHOLD SENDER
    // --------------------------
    private Mono<Void> sendThresholdAlert(Integer trainNo,
                                          String alertEmail,
                                          String metricName,
                                          Double latestValue,
                                          Double threshold,
                                          String unit) {
        if (latestValue <= threshold) {
            return Mono.empty();
        }

        String subj = String.format("Realtime Alert: %s Exceeded", metricName);
        String msg  = String.format(
                "Train %d: %s = %.2f %s (threshold %.2f %s)",
                trainNo, metricName, latestValue, unit, threshold, unit
        );
        Map<String,Object> payload = Map.of(
                "trainNo",   trainNo,
                "metric",    metricName,
                "value",     latestValue,
                "threshold", threshold,
                "unit",      unit,
                "timestamp", LocalDateTime.now()
        );

        return notificationService
                .sendAlert(alertEmail, subj, msg)
                .then(Mono.fromRunnable(() -> broadcaster.publish(payload, "ALERT")))
                .doOnSuccess(v -> log.info(
                        "Threshold alert sent for train {} metric {} value {}",
                        trainNo, metricName, latestValue
                ))
                .onErrorResume(ex -> {
                    log.error(
                            "Failed threshold alert for train {} metric {}: {}",
                            trainNo, metricName, ex.getMessage(), ex
                    );
                    return Mono.empty();
                }).then();
    }
}
