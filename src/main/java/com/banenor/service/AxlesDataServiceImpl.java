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
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Service implementation for streaming per-TP axle readings and computing global aggregations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AxlesDataServiceImpl implements AxlesDataService {

    private final RepositoryResolver repositoryResolver;

    // Pattern to match TP fields: spdTp1, aoaTp2, vviblTp3, etc.
    private static final Pattern TP_FIELD = Pattern.compile("^(spd|aoa|vvibl|vvibr|vfrcl|vfrcr|lfrcl|lfrcr|lvibl|lvibr)Tp(\\d+)$");

    /**
     * Streams each axle reading for the given train, time-window and measurementPoint (e.g. "TP1").
     */
    @Override
    public Flux<AxlesDataDTO> getAxlesData(Integer trainNo,
                                           LocalDateTime start,
                                           LocalDateTime end,
                                           String measurementPoint) {
        String idx = measurementPoint.replaceAll("(?i)tp", "").trim();
        log.debug("getAxlesData(trainNo={}, measurementPoint={}, start={}, end={})",
                trainNo, measurementPoint, start, end);

        return repositoryResolver.resolveRepository(trainNo)
                .flatMapMany(repo -> {
                    if (repo instanceof HaugfjellMP1AxlesRepository mp1) {
                        return mp1.findByTrainNoAndCreatedAtBetween(trainNo, start, end).cast(AbstractAxles.class);
                    } else if (repo instanceof HaugfjellMP3AxlesRepository mp3) {
                        return mp3.findByTrainNoAndCreatedAtBetween(trainNo, start, end).cast(AbstractAxles.class);
                    }
                    return Flux.error(new IllegalStateException("Unsupported repository for trainNo=" + trainNo));
                })
                .map(axle -> {
                    BeanWrapper bw = new BeanWrapperImpl(axle);
                    AxlesDataDTO.AxlesDataDTOBuilder b = AxlesDataDTO.builder()
                            .trainNo(axle.getHeader() != null ? axle.getHeader().getTrainNo() : trainNo)
                            .measurementPoint(measurementPoint)
                            .createdAt(axle.getCreatedAt());

                    applyField(bw, b, "spdTp" + idx,         b::speed);
                    applyField(bw, b, "aoaTp" + idx,         b::angleOfAttack);
                    applyField(bw, b, "vviblTp" + idx,       b::vibrationLeft);
                    applyField(bw, b, "vvibrTp" + idx,       b::vibrationRight);
                    applyField(bw, b, "vfrclTp" + idx,       b::verticalForceLeft);
                    applyField(bw, b, "vfrcrTp" + idx,       b::verticalForceRight);
                    applyField(bw, b, "lfrclTp" + idx,       b::lateralForceLeft);
                    applyField(bw, b, "lfrcrTp" + idx,       b::lateralForceRight);
                    applyField(bw, b, "lviblTp" + idx,       b::lateralVibrationLeft);
                    applyField(bw, b, "lvibrTp" + idx,       b::lateralVibrationRight);

                    return b.build();
                })
                .doOnError(e -> log.error("Error in getAxlesData: {}", e.getMessage(), e));
    }

    /**
     * Computes one global aggregation DTO across all trains for the given measurementPoint (e.g. "TP1").
     */
    @Override
    public Mono<AxlesDataDTO> getGlobalAggregations(String measurementPoint) {
        String idx = measurementPoint.replaceAll("(?i)tp", "").trim();
        log.debug("getGlobalAggregations(measurementPoint={})", measurementPoint);

        // helper: average one TP field across all repos
        Function<String, Mono<Double>> avgField = prefix ->
                repositoryResolver.resolveRepository(null)
                        .flatMapMany(repo -> {
                            // combined only supports findAll()
                            return repo.findAll().cast(AbstractAxles.class);
                        })
                        .flatMap(axle -> {
                            BeanWrapper bw = new BeanWrapperImpl(axle);
                            Object raw = bw.getPropertyValue(prefix + "Tp" + idx);
                            if (raw instanceof Number) {
                                return Mono.just(((Number) raw).doubleValue());
                            }
                            return Mono.empty();
                        })
                        .collectList()
                        .map(list -> list.isEmpty() ? 0.0 : list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        // prepare all ten aggregated monos
        List<Mono<Double>> monos = List.of(
                avgField.apply("spd"),
                avgField.apply("aoa"),
                avgField.apply("vvibl"),
                avgField.apply("vvibr"),
                avgField.apply("vfrcl"),
                avgField.apply("vfrcr"),
                avgField.apply("lfrcl"),
                avgField.apply("lfrcr"),
                avgField.apply("lvibl"),
                avgField.apply("lvibr")
        );

        // zip Iterable<Mono<?>> with a combinator
        return Mono.zip(monos, arr -> {
                    // arr is Object[]
                    return AxlesDataDTO.builder()
                            .trainNo(null)
                            .measurementPoint(measurementPoint)
                            .createdAt(LocalDateTime.now())
                            .speed((Double) arr[0])
                            .angleOfAttack((Double) arr[1])
                            .vibrationLeft((Double) arr[2])
                            .vibrationRight((Double) arr[3])
                            .verticalForceLeft((Double) arr[4])
                            .verticalForceRight((Double) arr[5])
                            .lateralForceLeft((Double) arr[6])
                            .lateralForceRight((Double) arr[7])
                            .lateralVibrationLeft((Double) arr[8])
                            .lateralVibrationRight((Double) arr[9])
                            .build();
                })
                .doOnError(e -> log.error("Error in getGlobalAggregations: {}", e.getMessage(), e));
    }

    /**
     * Reflectively reads a numeric property and applies it to the DTO builder.
     */
    private <T> void applyField(BeanWrapper bw,
                                AxlesDataDTO.AxlesDataDTOBuilder builder,
                                String propertyName,
                                Function<Double, AxlesDataDTO.AxlesDataDTOBuilder> setter) {
        Object val = bw.getPropertyValue(propertyName);
        setter.apply(val instanceof Number ? ((Number) val).doubleValue() : null);
    }
}
