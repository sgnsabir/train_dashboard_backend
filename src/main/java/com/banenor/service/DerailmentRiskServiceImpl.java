package com.banenor.service;

import com.banenor.config.InsightsProperties;
import com.banenor.dto.DerailmentRiskDTO;
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
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('MAINTENANCE')")
public class DerailmentRiskServiceImpl implements DerailmentRiskService {

    private final RepositoryResolver repositoryResolver;
    private final InsightsProperties insightsProperties;

    // matches vviblTpN, vvibrTpN, dtlTpN, dtrTpN
    private static final Pattern TP_FIELD = Pattern.compile("^(vvibl|vvibr|dtl|dtr)Tp(\\d+)$");

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
                    Flux<AbstractAxles> axleFlux;
                    if (repo instanceof HaugfjellMP1AxlesRepository mp1) {
                        axleFlux = mp1.findByTrainNoAndCreatedAtBetween(trainNo, start, end).cast(AbstractAxles.class);
                    } else if (repo instanceof HaugfjellMP3AxlesRepository mp3) {
                        axleFlux = mp3.findByTrainNoAndCreatedAtBetween(trainNo, start, end).cast(AbstractAxles.class);
                    } else {
                        return Flux.error(new IllegalArgumentException("Unsupported repository for trainNo=" + trainNo));
                    }
                    return axleFlux;
                })
                .flatMap(this::mapToAllTp)
                .doOnError(e -> log.error("Error in derailment risk analysis: {}", e.getMessage(), e));
    }

    private Flux<DerailmentRiskDTO> mapToAllTp(AbstractAxles axle) {
        BeanWrapper wrapper = new BeanWrapperImpl(axle);
        Map<Integer, BuilderHolder> holders = new TreeMap<>();

        for (PropertyDescriptor pd : wrapper.getPropertyDescriptors()) {
            Matcher m = TP_FIELD.matcher(pd.getName());
            if (!m.matches()) continue;

            String prop = m.group(1);           // vvibl, vvibr, dtl, dtr
            int tpIdx   = Integer.parseInt(m.group(2));
            Object raw  = wrapper.getPropertyValue(pd.getName());
            double val  = (raw instanceof Number) ? ((Number) raw).doubleValue() : 0.0;

            // init holder for this Tp if absent
            BuilderHolder h = holders.computeIfAbsent(tpIdx, idx -> {
                DerailmentRiskDTO.DerailmentRiskDTOBuilder bb = DerailmentRiskDTO.builder()
                        .trainNo(axle.getHeader() != null
                                ? axle.getHeader().getTrainNo()
                                : null)
                        .measurementTime(axle.getCreatedAt());
                return new BuilderHolder(bb);
            });

            switch (prop) {
                case "vvibl" -> h.vibrationLeft = val;
                case "vvibr" -> h.vibrationRight = val;
                case "dtl"   -> h.delayLeft      = val;
                case "dtr"   -> h.delayRight     = val;
            }
        }

        return Flux.fromIterable(
                holders.values().stream().map(h -> {
                    double maxVib = Math.max(h.vibrationLeft, h.vibrationRight);
                    double diff   = Math.abs(h.delayLeft - h.delayRight);

                    boolean vibRisk = maxVib > insightsProperties.getDerailment().getVibrationThreshold();
                    boolean dtRisk  = diff   > insightsProperties.getDerailment().getTimeDelayThreshold();
                    boolean detected = vibRisk || dtRisk;

                    StringBuilder msg = new StringBuilder();
                    if (vibRisk) msg.append(
                            String.format("Vibration (%.2f) exceeds threshold (%.2f). ",
                                    maxVib, insightsProperties.getDerailment().getVibrationThreshold())
                    );
                    if (dtRisk) msg.append(
                            String.format("Time‑delay difference (%.2f) exceeds threshold (%.2f).",
                                    diff, insightsProperties.getDerailment().getTimeDelayThreshold())
                    );
                    if (!detected) msg.append("Normal operation.");

                    return h.builder
                            .vibrationLeft(h.vibrationLeft)
                            .vibrationRight(h.vibrationRight)
                            .maxVibration(maxVib)
                            .timeDelayDifference(diff)
                            .riskDetected(detected)
                            .anomalyMessage(msg.toString().trim())
                            .build();
                }).toList()
        );
    }

    /** Helper to hold raw tp values before building the DTO. */
    private static class BuilderHolder {
        final DerailmentRiskDTO.DerailmentRiskDTOBuilder builder;
        double vibrationLeft  = 0.0;
        double vibrationRight = 0.0;
        double delayLeft      = 0.0;
        double delayRight     = 0.0;
        BuilderHolder(DerailmentRiskDTO.DerailmentRiskDTOBuilder b) { this.builder = b; }
    }
}
