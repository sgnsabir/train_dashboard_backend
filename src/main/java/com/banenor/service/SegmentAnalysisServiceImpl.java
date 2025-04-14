// src/main/java/com/banenor/service/SegmentAnalysisServiceImpl.java

package com.banenor.service;

import com.banenor.config.InsightsProperties;
import com.banenor.dto.SegmentAnalysisDTO;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('MAINTENANCE')")
public class SegmentAnalysisServiceImpl implements SegmentAnalysisService {

    private final RepositoryResolver repositoryResolver;
    private final InsightsProperties insightsProperties;

    // Define a hot spot threshold (e.g., if combined anomaly ratio meets or exceeds 50%)
    private static final double HOT_SPOT_PERCENTAGE_THRESHOLD = 0.5;

    @PostConstruct
    public void init() {
        log.info("SegmentAnalysisService initialized with thresholds: Vibration: {}, Lateral Force: {}, Vertical Force: {}",
                insightsProperties.getDerailment().getVibrationThreshold(),
                insightsProperties.getTrack().getHighLateralForce(),
                insightsProperties.getTrack().getHighVerticalForce());
    }

    @Override
    public Flux<SegmentAnalysisDTO> analyzeSegmentData(Integer trainNo, LocalDateTime start, LocalDateTime end) {
        // Retrieve sensor records from both MP1 and MP3 repositories.
        // Use flatMapMany() to convert the Mono returned by the repository resolver into a Flux.
        Flux<AbstractAxles> mp1Flux = repositoryResolver.resolveRepository(trainNo)
                .filter(repo -> repo instanceof HaugfjellMP1AxlesRepository)
                .flatMapMany(repo -> ((HaugfjellMP1AxlesRepository) repo)
                        .findByTrainNoAndCreatedAtBetween(trainNo, start, end));
        Flux<AbstractAxles> mp3Flux = repositoryResolver.resolveRepository(trainNo)
                .filter(repo -> repo instanceof HaugfjellMP3AxlesRepository)
                .flatMapMany(repo -> ((HaugfjellMP3AxlesRepository) repo)
                        .findByTrainNoAndCreatedAtBetween(trainNo, start, end));

        return Flux.merge(mp1Flux, mp3Flux)
                // Group records by segmentId; use 0 as the fallback key if segmentId is null.
                .groupBy(record -> record.getSegmentId() != null ? record.getSegmentId() : 0)
                .flatMap(group -> {
                    Integer segmentId = group.key();
                    return group.collectList().map(records -> analyzeSegment(records, segmentId));
                })
                .doOnError(ex -> log.error("Error during segment analysis: {}", ex.getMessage(), ex))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Analyzes a list of sensor records for a single segment.
     *
     * Aggregates the total record count and counts anomalies for:
     * - High vertical vibration (using left/right vibration values).
     * - High lateral force (using absolute left/right lateral force values).
     * - High vertical force (using vertical force values).
     *
     * Computes the anomaly ratio and flags the segment as a hot spot if the ratio meets or exceeds the threshold.
     *
     * @param records   List of sensor records for a segment.
     * @param segmentId The segment identifier (or 0 if null).
     * @return a SegmentAnalysisDTO with aggregated counts and hot spot flag.
     */
    private SegmentAnalysisDTO analyzeSegment(List<? extends AbstractAxles> records, Integer segmentId) {
        if (records == null || records.isEmpty()) {
            return SegmentAnalysisDTO.builder()
                    .segmentId(segmentId != null ? segmentId : 0)
                    .totalRecords(0)
                    .highVibrationCount(0)
                    .highLateralForceCount(0)
                    .highVerticalForceCount(0)
                    .hotSpot(false)
                    .build();
        }

        long totalRecords = records.size();
        long highVibrationCount = countHighVibration(records, insightsProperties.getDerailment().getVibrationThreshold());
        long highLateralForceCount = countHighForce(records, insightsProperties.getTrack().getHighLateralForce(), true);
        long highVerticalForceCount = countHighForce(records, insightsProperties.getTrack().getHighVerticalForce(), false);
        double anomalyRatio = (double) (highVibrationCount + highLateralForceCount + highVerticalForceCount) / totalRecords;
        boolean hotSpot = anomalyRatio >= HOT_SPOT_PERCENTAGE_THRESHOLD;

        return SegmentAnalysisDTO.builder()
                .segmentId(segmentId != null ? segmentId : 0)
                .totalRecords(totalRecords)
                .highVibrationCount(highVibrationCount)
                .highLateralForceCount(highLateralForceCount)
                .highVerticalForceCount(highVerticalForceCount)
                .hotSpot(hotSpot)
                .build();
    }

    /**
     * Counts the number of records with vertical vibration exceeding the threshold.
     *
     * @param records   List of sensor records.
     * @param threshold The vertical vibration threshold.
     * @return Count of records that exceed the threshold.
     */
    private long countHighVibration(List<? extends AbstractAxles> records, double threshold) {
        return records.stream().filter(record -> {
            double vibLeft = record.getVviblTp1() != null ? record.getVviblTp1() : 0.0;
            double vibRight = record.getVvibrTp1() != null ? record.getVvibrTp1() : 0.0;
            return Math.max(vibLeft, vibRight) > threshold;
        }).count();
    }

    /**
     * Counts the number of records with force (lateral or vertical) exceeding the threshold.
     *
     * @param records   List of sensor records.
     * @param threshold The force threshold.
     * @param absolute  True to compare absolute values (for lateral force), false for raw values (for vertical force).
     * @return Count of records that exceed the threshold.
     */
    private long countHighForce(List<? extends AbstractAxles> records, double threshold, boolean absolute) {
        return records.stream().filter(record -> {
            double forceLeft = record.getLfrclTp1() != null ? record.getLfrclTp1() : 0.0;
            double forceRight = record.getLfrcrTp1() != null ? record.getLfrcrTp1() : 0.0;
            if (absolute) {
                forceLeft = Math.abs(forceLeft);
                forceRight = Math.abs(forceRight);
            }
            return Math.max(forceLeft, forceRight) > threshold;
        }).count();
    }
}
