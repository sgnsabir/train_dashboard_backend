package com.banenor.service;

import com.banenor.config.InsightsProperties;
import com.banenor.dto.WheelConditionDTO;
import com.banenor.model.AbstractAxles;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.util.RepositoryResolver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('MAINTENANCE')")
public class WheelConditionServiceImpl implements WheelConditionService {

    private final RepositoryResolver repositoryResolver;
    private final InsightsProperties insightsProperties;

    @PostConstruct
    public void init() {
        log.info("WheelConditionService initialized with vibration flat threshold: {}",
                insightsProperties.getWheel().getVibrationFlatThreshold());
    }

    @Override
    public Flux<WheelConditionDTO> fetchWheelConditionData(Integer trainNo, LocalDateTime start, LocalDateTime end) {
        return repositoryResolver.resolveRepository(trainNo)
                .flatMapMany(repo -> {
                    if (repo instanceof HaugfjellMP1AxlesRepository) {
                        return ((HaugfjellMP1AxlesRepository) repo)
                                .findByTrainNoAndCreatedAtBetween(trainNo, start, end);
                    } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                        return ((HaugfjellMP3AxlesRepository) repo)
                                .findByTrainNoAndCreatedAtBetween(trainNo, start, end);
                    } else {
                        return Flux.error(new IllegalArgumentException("Unsupported repository type"));
                    }
                })
                // Pass the trainNo to the analysis method so that if the record header is missing,
                // we still have a fallback value.
                .map(record -> analyzeRecord(trainNo, record))
                .doOnError(ex -> log.error("Error in wheel condition analysis: {}", ex.getMessage(), ex))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Analyzes a single sensor record to determine if a potential wheel flat condition is detected.
     *
     * This method retrieves the vertical vibration measurements (left and right) from the sensor data,
     * compares them against the configured threshold (vibrationFlatThreshold),
     * and sets a flag if either exceeds the threshold.
     *
     * @param trainNo the train number provided as fallback if the record header is missing.
     * @param record  the sensor record.
     * @return a populated WheelConditionDTO with analysis results.
     */
    private WheelConditionDTO analyzeRecord(Integer trainNo, AbstractAxles record) {
        double verticalVibrationLeft = (record.getVviblTp1() != null) ? record.getVviblTp1() : 0.0;
        double verticalVibrationRight = (record.getVvibrTp1() != null) ? record.getVvibrTp1() : 0.0;
        double threshold = insightsProperties.getWheel().getVibrationFlatThreshold();
        boolean suspectedWheelFlat = verticalVibrationLeft > threshold || verticalVibrationRight > threshold;

        StringBuilder anomalyMessage = new StringBuilder();
        if (suspectedWheelFlat) {
            if (verticalVibrationLeft > threshold) {
                anomalyMessage.append(String.format("Left vertical vibration (%.2f) exceeds threshold (%.2f). ", verticalVibrationLeft, threshold));
            }
            if (verticalVibrationRight > threshold) {
                anomalyMessage.append(String.format("Right vertical vibration (%.2f) exceeds threshold (%.2f).", verticalVibrationRight, threshold));
            }
        } else {
            anomalyMessage.append("Wheel condition within normal parameters.");
        }

        // Use the record header's train number if available; otherwise fallback to the parameter "trainNo"
        Integer resolvedTrainNo = (record.getHeader() != null && record.getHeader().getTrainNo() != null)
                ? record.getHeader().getTrainNo()
                : trainNo;

        return WheelConditionDTO.builder()
                .trainNo(resolvedTrainNo)
                // Convert LocalDateTime to String (ISO format) for consistency
                .measurementTime(LocalDateTime.parse(record.getCreatedAt().toString()))
                .verticalVibrationLeft(verticalVibrationLeft)
                .verticalVibrationRight(verticalVibrationRight)
                .suspectedWheelFlat(suspectedWheelFlat)
                .anomalyMessage(anomalyMessage.toString().trim())
                .build();
    }
}
