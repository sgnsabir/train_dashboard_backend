package com.banenor.service;

import com.banenor.dto.AxlesDataDTO;
import com.banenor.model.AbstractAxles;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.util.RepositoryResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
public class AxlesDataService {

    private final RepositoryResolver repositoryResolver;

    // matches fields like spdTp1, aoaTp2, vviblTp3, vvibrTp4, vfrclTp2, etc.
    private static final Pattern TP_FIELD =
            Pattern.compile("^(spd|aoa|vvibl|vvibr|vfrcl|vfrcr|lfrcl|lfrcr|lvibl|lvibr)Tp(\\d+)$");

    /**
     * Retrieves axles data for a specific train number and time range,
     * emitting one AxlesDataDTO per test‑point found on each record.
     */
    public Flux<AxlesDataDTO> getAxlesData(Integer trainNo,
                                           LocalDateTime start,
                                           LocalDateTime end,
                                           String measurementPoint) {
        return repositoryResolver.resolveRepository(trainNo)
                .flatMapMany(repo -> {
                    Flux<AbstractAxles> axleFlux;
                    if (repo instanceof HaugfjellMP1AxlesRepository) {
                        HaugfjellMP1AxlesRepository mp1 = (HaugfjellMP1AxlesRepository) repo;
                        axleFlux = mp1.findByTrainNoAndCreatedAtBetween(trainNo, start, end)
                                .cast(AbstractAxles.class);
                    } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                        HaugfjellMP3AxlesRepository mp3 = (HaugfjellMP3AxlesRepository) repo;
                        axleFlux = mp3.findByTrainNoAndCreatedAtBetween(trainNo, start, end)
                                .cast(AbstractAxles.class);
                    } else {
                        return Flux.error(new IllegalArgumentException(
                                "Unsupported repository for trainNo=" + trainNo));
                    }

                    return axleFlux
                            .flatMap(axle -> mapToAllTp(axle, measurementPoint));
                })
                .onErrorResume(ex -> {
                    log.error("Error streaming axles data for train {}: {}", trainNo, ex.getMessage(), ex);
                    return Flux.empty();
                });
    }

    /**
     * Reflects on one AbstractAxles instance, discovers all Tp‑suffix fields,
     * groups them by Tp index, and builds one DTO per Tp.
     */
    private Flux<AxlesDataDTO> mapToAllTp(AbstractAxles axle, String measurementPoint) {
        BeanWrapper wrapper = new BeanWrapperImpl(axle);
        Map<Integer, AxlesDataDTO.AxlesDataDTOBuilder> builders = new TreeMap<>();

        for (PropertyDescriptor pd : wrapper.getPropertyDescriptors()) {
            String name = pd.getName();
            Matcher m = TP_FIELD.matcher(name);
            if (!m.matches()) continue;

            String prop = m.group(1);
            int tpIndex = Integer.parseInt(m.group(2));
            Object raw = wrapper.getPropertyValue(name);
            Double val = (raw instanceof Number) ? ((Number) raw).doubleValue() : null;

            AxlesDataDTO.AxlesDataDTOBuilder b = builders.computeIfAbsent(tpIndex, idx ->
                    AxlesDataDTO.builder()
                            .trainNo(axle.getHeader() != null ? axle.getHeader().getTrainNo() : null)
                            .measurementPoint(measurementPoint + ":TP" + idx)
                            .createdAt(axle.getCreatedAt())
            );

            switch (prop) {
                case "spd"   -> b.speed(val);
                case "aoa"   -> b.angleOfAttack(val);
                case "vvibl" -> b.vibrationLeft(val);
                case "vvibr" -> b.vibrationRight(val);
                case "vfrcl" -> b.verticalForceLeft(val);
                case "vfrcr" -> b.verticalForceRight(val);
                case "lfrcl" -> b.lateralForceLeft(val);
                case "lfrcr" -> b.lateralForceRight(val);
                case "lvibl" -> b.lateralVibrationLeft(val);
                case "lvibr" -> b.lateralVibrationRight(val);
            }
        }

        return Flux.fromIterable(
                builders.values().stream()
                        .map(AxlesDataDTO.AxlesDataDTOBuilder::build)
                        .collect(Collectors.toList())
        );
    }
}
