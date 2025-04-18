package com.banenor.service;

import com.banenor.config.InsightsProperties;
import com.banenor.dto.SteeringAlignmentDTO;
import com.banenor.model.AbstractAxles;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.repository.HaugfjellMP3HeaderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
public class SteeringAlignmentServiceImpl implements SteeringAlignmentService {

    private final HaugfjellMP1HeaderRepository mp1HeaderRepository;
    private final HaugfjellMP3HeaderRepository mp3HeaderRepository;
    private final HaugfjellMP1AxlesRepository mp1Repository;
    private final HaugfjellMP3AxlesRepository mp3Repository;
    private final InsightsProperties insightsProperties;

    private static final Pattern TP_FIELD = Pattern.compile("^(aoa|lfrcl|lfrcr|vfrcl|vfrcr)Tp(\\d+)$");

    @Override
    public Flux<SteeringAlignmentDTO> fetchSteeringData(Integer trainNo, LocalDateTime start, LocalDateTime end) {
        Mono<Flux<AbstractAxles>> mp1Flux = mp1HeaderRepository.findById(trainNo)
                .map(header -> mp1Repository.findByTrainNoAndCreatedAtBetween(trainNo, start, end).cast(AbstractAxles.class));
        Mono<Flux<AbstractAxles>> mp3Flux = mp3HeaderRepository.findById(trainNo)
                .map(header -> mp3Repository.findByTrainNoAndCreatedAtBetween(trainNo, start, end).cast(AbstractAxles.class));

        return Mono.zip(
                        mp1Flux.defaultIfEmpty(Flux.empty()),
                        mp3Flux.defaultIfEmpty(Flux.empty())
                )
                .flatMapMany(tuple -> Flux.merge(tuple.getT1(), tuple.getT2()))
                .flatMap(axle -> mapToAllTp(axle, trainNo))
                .doOnError(e -> log.error("Error in steering alignment analysis: {}", e.getMessage(), e));
    }

    private Flux<SteeringAlignmentDTO> mapToAllTp(AbstractAxles axle, Integer trainNo) {
        BeanWrapper wrapper = new BeanWrapperImpl(axle);
        Map<Integer, SteeringAlignmentDTO.SteeringAlignmentDTOBuilder> builders = new TreeMap<>();

        for (PropertyDescriptor pd : wrapper.getPropertyDescriptors()) {
            String name = pd.getName();
            Matcher m = TP_FIELD.matcher(name);
            if (!m.matches()) continue;

            String prop = m.group(1);
            int tpIndex = Integer.parseInt(m.group(2));
            Object raw = wrapper.getPropertyValue(name);
            double val = (raw instanceof Number) ? ((Number) raw).doubleValue() : 0.0;

            SteeringAlignmentDTO.SteeringAlignmentDTOBuilder b = builders.computeIfAbsent(tpIndex, idx ->
                    SteeringAlignmentDTO.builder()
                            .trainNo(axle.getHeader() != null ? axle.getHeader().getTrainNo() : trainNo)
                            .measurementTime(axle.getCreatedAt())
            );

            switch (prop) {
                case "aoa" -> b.angleOfAttack(Math.abs(val));
                case "lfrcl" -> b.lateralForceLeft(Math.abs(val));
                case "lfrcr" -> b.lateralForceRight(Math.abs(val));
                case "vfrcl" -> b.verticalForceLeft(val);
                case "vfrcr" -> b.verticalForceRight(val);
            }
        }

        return Flux.fromIterable(
                builders.values().stream().map(builder -> {
                    SteeringAlignmentDTO dto = builder.build();
                    boolean aoaOut = dto.getAngleOfAttack() > insightsProperties.getSteering().getAoaThreshold();
                    double totalVert = dto.getVerticalForceLeft() + dto.getVerticalForceRight();
                    double lateralSum = dto.getLateralForceLeft() + dto.getLateralForceRight();
                    double lrRatio = totalVert > 0 ? lateralSum / totalVert : 0.0;
                    boolean misaligned = lrRatio > insightsProperties.getSteering().getLvRatioThreshold();
                    StringBuilder msg = new StringBuilder();

                    if (aoaOut) {
                        msg.append(String.format("Angle of attack (%.2f) exceeds threshold (%.2f). ",
                                dto.getAngleOfAttack(), insightsProperties.getSteering().getAoaThreshold()));
                    }
                    if (misaligned) {
                        msg.append(String.format("Lateral/Vertical force ratio (%.2f) exceeds threshold (%.2f). ",
                                lrRatio, insightsProperties.getSteering().getLvRatioThreshold()));
                    }
                    if (!aoaOut && !misaligned) {
                        msg.append("Steering parameters are within acceptable ranges.");
                    }

                    return SteeringAlignmentDTO.builder()
                            .trainNo(dto.getTrainNo())
                            .measurementTime(dto.getMeasurementTime())
                            .angleOfAttack(dto.getAngleOfAttack())
                            .lateralForceLeft(dto.getLateralForceLeft())
                            .lateralForceRight(dto.getLateralForceRight())
                            .verticalForceLeft(dto.getVerticalForceLeft())
                            .verticalForceRight(dto.getVerticalForceRight())
                            .lateralVerticalRatio(lrRatio)
                            .aoaOutOfSpec(aoaOut)
                            .misalignmentDetected(misaligned)
                            .anomalyMessage(msg.toString().trim())
                            .build();
                }).collect(Collectors.toList())
        );
    }
}
