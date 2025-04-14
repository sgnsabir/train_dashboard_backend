package com.banenor.service;

import com.banenor.config.InsightsProperties;
import com.banenor.dto.TrackConditionDTO;
import com.banenor.model.AbstractAxles;
import com.banenor.model.HaugfjellMP1Header;
import com.banenor.model.HaugfjellMP3Header;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.util.RepositoryResolver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('MAINTENANCE')")
public class TrackConditionServiceImpl implements TrackConditionService {

    private final RepositoryResolver repositoryResolver;
    private final InsightsProperties insightsProperties;

    @PostConstruct
    public void init() {
        log.info("TrackConditionService initialized with high lateral force threshold: {} and high vertical force threshold: {}",
                insightsProperties.getTrack().getHighLateralForce(), insightsProperties.getTrack().getHighVerticalForce());
    }

    @Override
    public Flux<TrackConditionDTO> fetchTrackConditionData(Integer trainNo, LocalDateTime start, LocalDateTime end) {
        return repositoryResolver.resolveRepository(trainNo)
                .flatMapMany(repo -> {
                    // Since repo is a R2dbcRepository<?> without custom methods, we need to cast it
                    if (repo instanceof HaugfjellMP1AxlesRepository) {
                        return ((HaugfjellMP1AxlesRepository) repo).findByTrainNoAndCreatedAtBetween(trainNo, start, end);
                    } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                        return ((HaugfjellMP3AxlesRepository) repo).findByTrainNoAndCreatedAtBetween(trainNo, start, end);
                    } else {
                        return Flux.empty();
                    }
                })
                .map(this::analyzeRecord)
                .doOnError(ex -> log.error("Error in track condition analysis: {}", ex.getMessage(), ex));
    }

    /**
     * Analyzes a single sensor record to detect high lateral and vertical forces.
     * <p>
     * Evaluates:
     * <ul>
     *   <li>Lateral forces: Checks if the absolute value of left (lfrcl_tp1) or right (lfrcr_tp1) exceeds the configured threshold.</li>
     *   <li>Vertical forces: Checks if the absolute value of left (vfrcl_tp1) or right (vfrcr_tp1) exceeds the configured threshold.</li>
     * </ul>
     * </p>
     *
     * @param record a sensor record.
     * @return a populated TrackConditionDTO with analysis results.
     */
    private TrackConditionDTO analyzeRecord(AbstractAxles record) {
        // Retrieve lateral forces (using representative tp1 columns)
        double lateralForceLeft = record.getLfrclTp1() != null ? Math.abs(record.getLfrclTp1()) : 0.0;
        double lateralForceRight = record.getLfrcrTp1() != null ? Math.abs(record.getLfrcrTp1()) : 0.0;

        // Retrieve vertical forces (using representative tp1 columns)
        double verticalForceLeft = record.getVfrclTp1() != null ? Math.abs(record.getVfrclTp1()) : 0.0;
        double verticalForceRight = record.getVfrcrTp1() != null ? Math.abs(record.getVfrcrTp1()) : 0.0;

        // Thresholds from configuration
        double highLateralThreshold = insightsProperties.getTrack().getHighLateralForce();
        double highVerticalThreshold = insightsProperties.getTrack().getHighVerticalForce();

        // Determine if the forces exceed thresholds
        boolean isHighLateral = lateralForceLeft > highLateralThreshold || lateralForceRight > highLateralThreshold;
        boolean isHighVertical = verticalForceLeft > highVerticalThreshold || verticalForceRight > highVerticalThreshold;

        // Build anomaly message
        StringBuilder anomalyMessage = new StringBuilder();
        if (isHighLateral) {
            if (lateralForceLeft > highLateralThreshold) {
                anomalyMessage.append(String.format("Left lateral force (%.2f) exceeds threshold (%.2f). ", lateralForceLeft, highLateralThreshold));
            }
            if (lateralForceRight > highLateralThreshold) {
                anomalyMessage.append(String.format("Right lateral force (%.2f) exceeds threshold (%.2f). ", lateralForceRight, highLateralThreshold));
            }
        }
        if (isHighVertical) {
            if (verticalForceLeft > highVerticalThreshold) {
                anomalyMessage.append(String.format("Left vertical force (%.2f) exceeds threshold (%.2f). ", verticalForceLeft, highVerticalThreshold));
            }
            if (verticalForceRight > highVerticalThreshold) {
                anomalyMessage.append(String.format("Right vertical force (%.2f) exceeds threshold (%.2f).", verticalForceRight, highVerticalThreshold));
            }
        }
        if (!isHighLateral && !isHighVertical) {
            anomalyMessage.append("Track conditions are within normal parameters.");
        }

        // Resolve train number from the header if available
        Integer resolvedTrainNo = record.getHeader() != null ? record.getHeader().getTrainNo() : null;

        return TrackConditionDTO.builder()
                .trainNo(resolvedTrainNo)
                .measurementTime(record.getCreatedAt())
                .lateralForceLeft(lateralForceLeft)
                .lateralForceRight(lateralForceRight)
                .verticalForceLeft(verticalForceLeft)
                .verticalForceRight(verticalForceRight)
                .highLateralForce(isHighLateral)
                .highVerticalForce(isHighVertical)
                .anomalyMessage(anomalyMessage.toString().trim())
                .build();
    }
}
