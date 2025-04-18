package com.banenor.service;

import com.banenor.config.InsightsProperties;
import com.banenor.dto.TrackConditionDTO;
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

import java.beans.PropertyDescriptor;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('MAINTENANCE')")
public class TrackConditionServiceImpl implements TrackConditionService {

    private final RepositoryResolver repositoryResolver;
    private final InsightsProperties insightsProperties;

    // Matches only the 4 fields we care about, for any Tp index
    private static final Pattern TP_FIELD = Pattern.compile("^(lfrcl|lfrcr|vfrcl|vfrcr)Tp(\\d+)$");

    @PostConstruct
    public void init() {
        log.info("TrackConditionService initialized with lateral threshold={} and vertical threshold={}",
                insightsProperties.getTrack().getHighLateralForce(),
                insightsProperties.getTrack().getHighVerticalForce());
    }

    @Override
    public Flux<TrackConditionDTO> fetchTrackConditionData(Integer trainNo, LocalDateTime start, LocalDateTime end) {
        return repositoryResolver.resolveRepository(trainNo)
                .flatMapMany(repo -> {
                    if (repo instanceof HaugfjellMP1AxlesRepository) {
                        return ((HaugfjellMP1AxlesRepository) repo).findByTrainNoAndCreatedAtBetween(trainNo, start, end);
                    } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                        return ((HaugfjellMP3AxlesRepository) repo).findByTrainNoAndCreatedAtBetween(trainNo, start, end);
                    } else {
                        return Flux.empty();
                    }
                })
                .flatMap(this::mapToDynamicTp)
                .doOnError(ex -> log.error("Error in track condition analysis: {}", ex.getMessage(), ex));
    }

    /**
     * Splits one AbstractAxles record into one DTO per Tp index by reflection.
     */
    private Flux<TrackConditionDTO> mapToDynamicTp(AbstractAxles record) {
        BeanWrapper wrapper = new BeanWrapperImpl(record);
        Map<Integer, Map<String, Double>> tpMap = new TreeMap<>();

        // Collect per‑Tp values
        for (PropertyDescriptor pd : wrapper.getPropertyDescriptors()) {
            String name = pd.getName();
            Matcher m = TP_FIELD.matcher(name);
            if (!m.matches()) continue;

            String prop = m.group(1);           // e.g. "lfrcl"
            int tp = Integer.parseInt(m.group(2)); // e.g. 1,2,...

            Object raw = wrapper.getPropertyValue(name);
            double val = (raw instanceof Number)
                    ? Math.abs(((Number) raw).doubleValue())
                    : 0.0;

            tpMap.computeIfAbsent(tp, k -> new HashMap<>()).put(prop, val);
        }

        return Flux.fromIterable(tpMap.entrySet())
                .map(entry -> {
                    int tp = entry.getKey();
                    Map<String, Double> vals = entry.getValue();

                    double lateralLeft   = vals.getOrDefault("lfrcl", 0.0);
                    double lateralRight  = vals.getOrDefault("lfrcr", 0.0);
                    double verticalLeft  = vals.getOrDefault("vfrcl", 0.0);
                    double verticalRight = vals.getOrDefault("vfrcr", 0.0);

                    double latThr = insightsProperties.getTrack().getHighLateralForce();
                    double vertThr = insightsProperties.getTrack().getHighVerticalForce();

                    boolean highLat  = lateralLeft  > latThr || lateralRight  > latThr;
                    boolean highVert = verticalLeft > vertThr || verticalRight > vertThr;

                    StringBuilder anomaly = new StringBuilder();
                    if (highLat) {
                        if (lateralLeft > latThr)
                            anomaly.append(String.format("Left lateral force (%.2f) exceeds threshold (%.2f). ",
                                    lateralLeft, latThr));
                        if (lateralRight > latThr)
                            anomaly.append(String.format("Right lateral force (%.2f) exceeds threshold (%.2f). ",
                                    lateralRight, latThr));
                    }
                    if (highVert) {
                        if (verticalLeft > vertThr)
                            anomaly.append(String.format("Left vertical force (%.2f) exceeds threshold (%.2f). ",
                                    verticalLeft, vertThr));
                        if (verticalRight > vertThr)
                            anomaly.append(String.format("Right vertical force (%.2f) exceeds threshold (%.2f).",
                                    verticalRight, vertThr));
                    }
                    if (!highLat && !highVert) {
                        anomaly.append("Track conditions are within normal parameters.");
                    }

                    Integer resolvedTrainNo = (record.getHeader() != null)
                            ? record.getHeader().getTrainNo()
                            : null;

                    return TrackConditionDTO.builder()
                            .trainNo(resolvedTrainNo)
                            .measurementTime(record.getCreatedAt())
                            .measurementPoint("TP" + tp)
                            .lateralForceLeft(lateralLeft)
                            .lateralForceRight(lateralRight)
                            .verticalForceLeft(verticalLeft)
                            .verticalForceRight(verticalRight)
                            .highLateralForce(highLat)
                            .highVerticalForce(highVert)
                            .anomalyMessage(anomaly.toString().trim())
                            .build();
                });
    }
}
