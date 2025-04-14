package com.banenor.service;

import com.banenor.alert.NotificationService;
import com.banenor.config.MaintenanceProperties;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.util.RepositoryResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeAlertServiceImpl implements RealtimeAlertService {

    private final RepositoryResolver repositoryResolver;
    private final NotificationService notificationService;
    private final MaintenanceProperties maintenanceProperties;

    /**
     * Returns the provided Mono or an empty Mono if null.
     */
    private <T> Mono<T> safeMono(Mono<T> mono) {
        return mono != null ? mono : Mono.empty();
    }

    /**
     * Helper function to safely cast an object to Double.
     */
    private final Function<Object, Double> toDouble = obj ->
            (obj instanceof Number) ? ((Number) obj).doubleValue() : 0.0;

    /**
     * Helper method to build and send an alert email for a specific metric.
     */
    private Mono<Void> sendThresholdAlert(String alertEmail, String metricName, Double latestValue, Double threshold, String unit) {
        if (latestValue > threshold) {
            String subject = String.format("Realtime Alert: %s Exceeded", metricName);
            String message = String.format("Alert: Latest %s (%.2f %s) exceeds threshold (%.2f %s).",
                    metricName, latestValue, unit, threshold, unit);
            return notificationService.sendAlert(alertEmail, subject, message).then();
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> monitorAndAlert(Integer trainNo, String alertEmail) {
        return repositoryResolver.resolveRepository(trainNo)
                .flatMap(repo -> {
                    if (repo instanceof HaugfjellMP1AxlesRepository) {
                        return processAlertsForMP1((HaugfjellMP1AxlesRepository) repo, trainNo, alertEmail);
                    } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                        return processAlertsForMP3((HaugfjellMP3AxlesRepository) repo, trainNo, alertEmail);
                    } else {
                        return Mono.error(new IllegalArgumentException("Unsupported repository type for train number: " + trainNo));
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> processAlertsForMP1(HaugfjellMP1AxlesRepository repo, Integer trainNo, String alertEmail) {
        Mono<Void> speedAlert = safeMono(
                repo.findFirstSpdTp1ByTrainNoOrderByCreatedAtDesc(trainNo)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> {
            Double latestSpeed = toDouble.apply(latest);
            return sendThresholdAlert(alertEmail, "Speed", latestSpeed,
                    maintenanceProperties.getRealtimeSpeedThreshold(), "km/h");
        }).onErrorResume(e -> {
            log.error("Error processing speed alert for trainNo {}: {}", trainNo, e.getMessage());
            return Mono.empty();
        });

        Mono<Void> vibrationLeftAlert = safeMono(
                repo.findFirstVviblTp1ByTrainNoOrderByCreatedAtDesc(trainNo)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> {
            Double latestVibrationLeft = toDouble.apply(latest);
            return sendThresholdAlert(alertEmail, "Left Vibration", latestVibrationLeft,
                    maintenanceProperties.getRealtimeVibrationLeftThreshold(), "");
        }).onErrorResume(e -> {
            log.error("Error processing left vibration alert for trainNo {}: {}", trainNo, e.getMessage());
            return Mono.empty();
        });

        Mono<Void> vibrationRightAlert = safeMono(
                repo.findFirstVvibrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> {
            Double latestVibrationRight = toDouble.apply(latest);
            return sendThresholdAlert(alertEmail, "Right Vibration", latestVibrationRight,
                    maintenanceProperties.getRealtimeVibrationRightThreshold(), "");
        }).onErrorResume(e -> {
            log.error("Error processing right vibration alert for trainNo {}: {}", trainNo, e.getMessage());
            return Mono.empty();
        });

        Mono<Void> verticalForceRightAlert = safeMono(
                repo.findFirstVfrcrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> {
            Double latestVerticalForceRight = toDouble.apply(latest);
            return sendThresholdAlert(alertEmail, "Vertical Force Right", latestVerticalForceRight,
                    maintenanceProperties.getRealtimeVerticalForceRightThreshold(), "kN");
        }).onErrorResume(e -> {
            log.error("Error processing vertical force right alert for trainNo {}: {}", trainNo, e.getMessage());
            return Mono.empty();
        });

        Mono<Void> verticalForceLeftAlert = safeMono(
                repo.findFirstVfrclTp1ByTrainNoOrderByCreatedAtDesc(trainNo)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> {
            Double latestVerticalForceLeft = toDouble.apply(latest);
            return sendThresholdAlert(alertEmail, "Vertical Force Left", latestVerticalForceLeft,
                    maintenanceProperties.getRealtimeVerticalForceLeftThreshold(), "kN");
        }).onErrorResume(e -> {
            log.error("Error processing vertical force left alert for trainNo {}: {}", trainNo, e.getMessage());
            return Mono.empty();
        });

        Mono<Void> lateralForceRightAlert = safeMono(
                repo.findFirstLfrcrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> {
            Double latestLateralForceRight = toDouble.apply(latest);
            return sendThresholdAlert(alertEmail, "Lateral Force Right", latestLateralForceRight,
                    maintenanceProperties.getRealtimeLateralForceRightThreshold(), "kN");
        }).onErrorResume(e -> {
            log.error("Error processing lateral force right alert for trainNo {}: {}", trainNo, e.getMessage());
            return Mono.empty();
        });

        Mono<Void> lateralVibrationRightAlert = safeMono(
                repo.findFirstLvibrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> {
            Double latestLateralVibrationRight = toDouble.apply(latest);
            return sendThresholdAlert(alertEmail, "Lateral Vibration Right", latestLateralVibrationRight,
                    maintenanceProperties.getRealtimeLateralVibrationRightThreshold(), "");
        }).onErrorResume(e -> {
            log.error("Error processing lateral vibration right alert for trainNo {}: {}", trainNo, e.getMessage());
            return Mono.empty();
        });

        return Mono.whenDelayError(speedAlert,
                vibrationLeftAlert,
                vibrationRightAlert,
                verticalForceRightAlert,
                verticalForceLeftAlert,
                lateralForceRightAlert,
                lateralVibrationRightAlert);
    }

    private Mono<Void> processAlertsForMP3(HaugfjellMP3AxlesRepository repo, Integer trainNo, String alertEmail) {
        // Similar alert processing for MP3 repository
        Mono<Void> speedAlert = safeMono(
                repo.findFirstSpdTp1ByTrainNoOrderByCreatedAtDesc(trainNo)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> {
            Double latestSpeed = toDouble.apply(latest);
            return sendThresholdAlert(alertEmail, "Speed", latestSpeed,
                    maintenanceProperties.getRealtimeSpeedThreshold(), "km/h");
        }).onErrorResume(e -> {
            log.error("Error processing speed alert for trainNo {}: {}", trainNo, e.getMessage());
            return Mono.empty();
        });

        Mono<Void> vibrationLeftAlert = safeMono(
                repo.findFirstVviblTp1ByTrainNoOrderByCreatedAtDesc(trainNo)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> {
            Double latestVibrationLeft = toDouble.apply(latest);
            return sendThresholdAlert(alertEmail, "Left Vibration", latestVibrationLeft,
                    maintenanceProperties.getRealtimeVibrationLeftThreshold(), "");
        }).onErrorResume(e -> {
            log.error("Error processing left vibration alert for trainNo {}: {}", trainNo, e.getMessage());
            return Mono.empty();
        });

        Mono<Void> vibrationRightAlert = safeMono(
                repo.findFirstVvibrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> {
            Double latestVibrationRight = toDouble.apply(latest);
            return sendThresholdAlert(alertEmail, "Right Vibration", latestVibrationRight,
                    maintenanceProperties.getRealtimeVibrationRightThreshold(), "");
        }).onErrorResume(e -> {
            log.error("Error processing right vibration alert for trainNo {}: {}", trainNo, e.getMessage());
            return Mono.empty();
        });

        Mono<Void> verticalForceRightAlert = safeMono(
                repo.findFirstVfrcrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> {
            Double latestVerticalForceRight = toDouble.apply(latest);
            return sendThresholdAlert(alertEmail, "Vertical Force Right", latestVerticalForceRight,
                    maintenanceProperties.getRealtimeVerticalForceRightThreshold(), "kN");
        }).onErrorResume(e -> {
            log.error("Error processing vertical force right alert for trainNo {}: {}", trainNo, e.getMessage());
            return Mono.empty();
        });

        Mono<Void> verticalForceLeftAlert = safeMono(
                repo.findFirstVfrclTp1ByTrainNoOrderByCreatedAtDesc(trainNo)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> {
            Double latestVerticalForceLeft = toDouble.apply(latest);
            return sendThresholdAlert(alertEmail, "Vertical Force Left", latestVerticalForceLeft,
                    maintenanceProperties.getRealtimeVerticalForceLeftThreshold(), "kN");
        }).onErrorResume(e -> {
            log.error("Error processing vertical force left alert for trainNo {}: {}", trainNo, e.getMessage());
            return Mono.empty();
        });

        Mono<Void> lateralForceRightAlert = safeMono(
                repo.findFirstLfrcrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> {
            Double latestLateralForceRight = toDouble.apply(latest);
            return sendThresholdAlert(alertEmail, "Lateral Force Right", latestLateralForceRight,
                    maintenanceProperties.getRealtimeLateralForceRightThreshold(), "kN");
        }).onErrorResume(e -> {
            log.error("Error processing lateral force right alert for trainNo {}: {}", trainNo, e.getMessage());
            return Mono.empty();
        });

        Mono<Void> lateralVibrationRightAlert = safeMono(
                repo.findFirstLvibrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)
                        .defaultIfEmpty(0.0)
        ).flatMap(latest -> {
            Double latestLateralVibrationRight = toDouble.apply(latest);
            return sendThresholdAlert(alertEmail, "Lateral Vibration Right", latestLateralVibrationRight,
                    maintenanceProperties.getRealtimeLateralVibrationRightThreshold(), "");
        }).onErrorResume(e -> {
            log.error("Error processing lateral vibration right alert for trainNo {}: {}", trainNo, e.getMessage());
            return Mono.empty();
        });

        return Mono.whenDelayError(speedAlert,
                vibrationLeftAlert,
                vibrationRightAlert,
                verticalForceRightAlert,
                verticalForceLeftAlert,
                lateralForceRightAlert,
                lateralVibrationRightAlert);
    }
}
