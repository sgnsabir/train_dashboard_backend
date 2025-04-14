package com.banenor.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.banenor.config.InsightsProperties;
import com.banenor.dto.DerailmentRiskDTO;
import com.banenor.model.AbstractAxles;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.util.RepositoryResolver;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Implementation of the DerailmentRiskService.
 *
 * This service fetches sensor data for a given train number and time range using a RepositoryResolver,
 * then analyzes each record to flag potential derailment risks based on:
 * <ul>
 *   <li>Vibration spikes – if the maximum of left/right vertical vibrations exceeds a configured threshold.</li>
 *   <li>Time delay differences – if the absolute difference between left and right time delays exceeds a threshold.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('MAINTENANCE')")
public class DerailmentRiskServiceImpl implements DerailmentRiskService {

    private final RepositoryResolver repositoryResolver;
    private final InsightsProperties insightsProperties;

    @PostConstruct
    public void init() {
        log.info("DerailmentRiskService initialized with vibration threshold {} and time delay threshold {}",
                insightsProperties.getDerailment().getVibrationThreshold(),
                insightsProperties.getDerailment().getTimeDelayThreshold());
    }

    @Override
    public Flux<DerailmentRiskDTO> fetchDerailmentRiskData(Integer trainNo, LocalDateTime start, LocalDateTime end) {
        return repositoryResolver.resolveRepository(trainNo)
                .flatMapMany(repo -> {
                    if (repo instanceof HaugfjellMP1AxlesRepository) {
                        return ((HaugfjellMP1AxlesRepository) repo)
                                .findByTrainNoAndCreatedAtBetween(trainNo, start, end);
                    } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                        return ((HaugfjellMP3AxlesRepository) repo)
                                .findByTrainNoAndCreatedAtBetween(trainNo, start, end);
                    } else {
                        return Flux.error(new IllegalArgumentException("Repository type not recognized"));
                    }
                })
                .map(this::analyzeRecord)
                .doOnError(ex -> log.error("Error in derailment risk analysis: {}", ex.getMessage(), ex));
    }

    /**
     * Analyzes a single sensor record to detect derailment risk.
     *
     * Checks include:
     * <ul>
     *   <li>Vibration Spike – evaluates the maximum vertical vibration (left/right)
     *       against the configured vibration threshold.</li>
     *   <li>Time Delay Difference – calculates the absolute difference between
     *       left and right time delays and compares it with the configured threshold.</li>
     * </ul>
     *
     * @param record a sensor record.
     * @return a populated DerailmentRiskDTO reflecting the analysis.
     */
    private DerailmentRiskDTO analyzeRecord(AbstractAxles record) {
        double vibrationLeft = Optional.ofNullable(record.getVviblTp1()).orElse(0.0);
        double vibrationRight = Optional.ofNullable(record.getVvibrTp1()).orElse(0.0);
        double maxVibration = Math.max(vibrationLeft, vibrationRight);

        double timeDelayLeft = Optional.ofNullable(record.getDtlTp1()).orElse(0.0);
        double timeDelayRight = Optional.ofNullable(record.getDtrTp1()).orElse(0.0);
        double timeDelayDifference = Math.abs(timeDelayLeft - timeDelayRight);

        boolean vibrationRisk = maxVibration > insightsProperties.getDerailment().getVibrationThreshold();
        boolean timeDelayRisk = timeDelayDifference > insightsProperties.getDerailment().getTimeDelayThreshold();
        boolean riskDetected = vibrationRisk || timeDelayRisk;

        StringBuilder anomalyMessage = new StringBuilder();
        if (vibrationRisk) {
            anomalyMessage.append(String.format("Vibration (%.2f) exceeds threshold (%.2f). ",
                    maxVibration, insightsProperties.getDerailment().getVibrationThreshold()));
        }
        if (timeDelayRisk) {
            anomalyMessage.append(String.format("Time delay difference (%.2f) exceeds threshold (%.2f).",
                    timeDelayDifference, insightsProperties.getDerailment().getTimeDelayThreshold()));
        }
        if (!riskDetected) {
            anomalyMessage.append("Normal operation.");
        }

        Integer resolvedTrainNo = (record.getHeader() != null) ? record.getHeader().getTrainNo() : null;

        return DerailmentRiskDTO.builder()
                .trainNo(resolvedTrainNo)
                .measurementTime(record.getCreatedAt())
                .vibrationLeft(vibrationLeft)
                .vibrationRight(vibrationRight)
                .maxVibration(maxVibration)
                .timeDelayDifference(timeDelayDifference)
                .riskDetected(riskDetected)
                .anomalyMessage(anomalyMessage.toString().trim())
                .build();
    }
}
