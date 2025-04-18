package com.banenor.service;

import com.banenor.config.InsightsProperties;
import com.banenor.dto.WheelConditionDTO;
import com.banenor.model.AbstractAxles;
import com.banenor.util.RepositoryResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;
import java.beans.PropertyDescriptor;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('MAINTENANCE')")
public class WheelConditionServiceImpl implements WheelConditionService {

    private final RepositoryResolver repositoryResolver;
    private final InsightsProperties insightsProperties;

    private static final Pattern TP_FIELD = Pattern.compile("^(vvibl|vvibr)Tp(\\d+)$");

    @PostConstruct
    public void init() {
        log.info("WheelConditionService initialized with vibration flat threshold: {}",
                insightsProperties.getWheel().getVibrationFlatThreshold());
    }

    @Override
    public Flux<WheelConditionDTO> fetchWheelConditionData(Integer trainNo, LocalDateTime start, LocalDateTime end) {
        return repositoryResolver.resolveRepository(trainNo)
                .flatMapMany(repo -> {
                    if (repo instanceof com.banenor.repository.HaugfjellMP1AxlesRepository) {
                        return ((com.banenor.repository.HaugfjellMP1AxlesRepository) repo)
                                .findByTrainNoAndCreatedAtBetween(trainNo, start, end);
                    } else if (repo instanceof com.banenor.repository.HaugfjellMP3AxlesRepository) {
                        return ((com.banenor.repository.HaugfjellMP3AxlesRepository) repo)
                                .findByTrainNoAndCreatedAtBetween(trainNo, start, end);
                    } else {
                        return Flux.error(new IllegalArgumentException("Unsupported repository type"));
                    }
                })
                .flatMap(axle -> mapToAllTp(axle, trainNo))
                .doOnError(ex -> log.error("Error in wheel condition analysis: {}", ex.getMessage(), ex))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<WheelConditionDTO> mapToAllTp(AbstractAxles axle, Integer fallbackTrainNo) {
        BeanWrapper wrapper = new BeanWrapperImpl(axle);
        Map<Integer, WheelConditionDTO.WheelConditionDTOBuilder> builders = new TreeMap<>();

        for (PropertyDescriptor pd : wrapper.getPropertyDescriptors()) {
            String name = pd.getName();
            Matcher m = TP_FIELD.matcher(name);
            if (!m.matches()) continue;

            String prop = m.group(1); // vvibl or vvibr
            int tpIndex = Integer.parseInt(m.group(2));
            Object raw = wrapper.getPropertyValue(name);
            Double val = (raw instanceof Number) ? ((Number) raw).doubleValue() : 0.0;

            WheelConditionDTO.WheelConditionDTOBuilder b = builders.computeIfAbsent(tpIndex, idx ->
                    WheelConditionDTO.builder()
                            .trainNo(axle.getHeader() != null ? axle.getHeader().getTrainNo() : fallbackTrainNo)
                            .measurementTime(axle.getCreatedAt())
            );

            if ("vvibl".equals(prop)) b.verticalVibrationLeft(val);
            else if ("vvibr".equals(prop)) b.verticalVibrationRight(val);
        }

        return Flux.fromIterable(
                builders.values().stream().map(b -> {
                    WheelConditionDTO dto = b.build();
                    double left = dto.getVerticalVibrationLeft();
                    double right = dto.getVerticalVibrationRight();
                    boolean suspected = left > insightsProperties.getWheel().getVibrationFlatThreshold()
                            || right > insightsProperties.getWheel().getVibrationFlatThreshold();
                    StringBuilder msg = new StringBuilder();
                    if (suspected) {
                        if (left > insightsProperties.getWheel().getVibrationFlatThreshold()) {
                            msg.append(String.format("Left vertical vibration (%.2f) exceeds threshold (%.2f). ",
                                    left, insightsProperties.getWheel().getVibrationFlatThreshold()));
                        }
                        if (right > insightsProperties.getWheel().getVibrationFlatThreshold()) {
                            msg.append(String.format("Right vertical vibration (%.2f) exceeds threshold (%.2f).",
                                    right, insightsProperties.getWheel().getVibrationFlatThreshold()));
                        }
                    } else {
                        msg.append("Wheel condition within normal parameters.");
                    }
                    return WheelConditionDTO.builder()
                            .trainNo(dto.getTrainNo())
                            .measurementTime(dto.getMeasurementTime())
                            .verticalVibrationLeft(dto.getVerticalVibrationLeft())
                            .verticalVibrationRight(dto.getVerticalVibrationRight())
                            .suspectedWheelFlat(suspected)
                            .anomalyMessage(msg.toString().trim())
                            .build();
                }).collect(Collectors.toList())
        );
    }
}
