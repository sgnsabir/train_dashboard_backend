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
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.beans.PropertyDescriptor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('MAINTENANCE')")
public class SegmentAnalysisServiceImpl implements SegmentAnalysisService {

    private final RepositoryResolver repositoryResolver;
    private final InsightsProperties insightsProperties;

    private static final double HOT_SPOT_PERCENTAGE_THRESHOLD = 0.5;

    // patterns for Tp fields
    private static final Pattern VIBRATION_PATTERN = Pattern.compile("^(vvibl|vvibr)Tp\\d+$");
    private static final Pattern LATERAL_PATTERN   = Pattern.compile("^(lfrcl|lfrcr)Tp\\d+$");
    private static final Pattern VERTICAL_PATTERN  = Pattern.compile("^(vfrcl|vfrcr)Tp\\d+$");

    @PostConstruct
    public void init() {
        log.info("SegmentAnalysisService initialized with thresholds: Vibration={}, LateralForce={}, VerticalForce={}",
                insightsProperties.getDerailment().getVibrationThreshold(),
                insightsProperties.getTrack().getHighLateralForce(),
                insightsProperties.getTrack().getHighVerticalForce());
    }

    @Override
    public Flux<SegmentAnalysisDTO> analyzeSegmentData(Integer trainNo, LocalDateTime start, LocalDateTime end) {
        Flux<AbstractAxles> data = repositoryResolver.resolveRepository(trainNo)
                .flatMapMany(repo -> {
                    if (repo instanceof HaugfjellMP1AxlesRepository) {
                        return ((HaugfjellMP1AxlesRepository) repo)
                                .findByTrainNoAndCreatedAtBetween(trainNo, start, end)
                                .cast(AbstractAxles.class);
                    } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                        return ((HaugfjellMP3AxlesRepository) repo)
                                .findByTrainNoAndCreatedAtBetween(trainNo, start, end)
                                .cast(AbstractAxles.class);
                    } else {
                        return Flux.error(new IllegalArgumentException("No Axles repository found for trainNo: " + trainNo));
                    }
                });

        return data
                .groupBy(axle -> axle.getSegmentId() != null ? axle.getSegmentId() : 0)
                .flatMap(group -> group.collectList()
                        .map(records -> analyzeSegment(records, group.key())))
                .doOnError(e -> log.error("Error during segment analysis: {}", e.getMessage(), e))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private SegmentAnalysisDTO analyzeSegment(List<? extends AbstractAxles> records, Integer segmentId) {
        if (records.isEmpty()) {
            return SegmentAnalysisDTO.builder()
                    .segmentId(segmentId)
                    .totalRecords(0)
                    .highVibrationCount(0)
                    .highLateralForceCount(0)
                    .highVerticalForceCount(0)
                    .hotSpot(false)
                    .build();
        }

        long total = records.size();
        long vibCount = records.stream()
                .filter(r -> isAboveThreshold(r, VIBRATION_PATTERN, insightsProperties.getDerailment().getVibrationThreshold(), false))
                .count();
        long latCount = records.stream()
                .filter(r -> isAboveThreshold(r, LATERAL_PATTERN, insightsProperties.getTrack().getHighLateralForce(), true))
                .count();
        long vertCount = records.stream()
                .filter(r -> isAboveThreshold(r, VERTICAL_PATTERN, insightsProperties.getTrack().getHighVerticalForce(), false))
                .count();

        double ratio = (double)(vibCount + latCount + vertCount) / total;
        boolean hotSpot = ratio >= HOT_SPOT_PERCENTAGE_THRESHOLD;

        return SegmentAnalysisDTO.builder()
                .segmentId(segmentId)
                .totalRecords(total)
                .highVibrationCount(vibCount)
                .highLateralForceCount(latCount)
                .highVerticalForceCount(vertCount)
                .hotSpot(hotSpot)
                .build();
    }

    private boolean isAboveThreshold(AbstractAxles axle, Pattern pattern, double threshold, boolean absolute) {
        BeanWrapper wrapper = new BeanWrapperImpl(axle);
        double max = 0.0;
        for (PropertyDescriptor pd : wrapper.getPropertyDescriptors()) {
            String name = pd.getName();
            if (!pattern.matcher(name).matches()) continue;
            Object raw = wrapper.getPropertyValue(name);
            double val = (raw instanceof Number) ? ((Number) raw).doubleValue() : 0.0;
            if (absolute) val = Math.abs(val);
            if (val > max) max = val;
        }
        return max > threshold;
    }
}
