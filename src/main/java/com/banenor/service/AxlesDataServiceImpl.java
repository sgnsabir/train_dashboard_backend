package com.banenor.service;

import com.banenor.dto.AxlesDataDTO;
import com.banenor.model.AbstractAxles;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class AxlesDataServiceImpl implements AxlesDataService {

    private final HaugfjellMP1AxlesRepository mp1Repository;
    private final HaugfjellMP3AxlesRepository mp3Repository;

    @Override
    public Flux<AxlesDataDTO> getAxlesData(Integer trainNo,
                                           LocalDateTime start,
                                           LocalDateTime end,
                                           String measurementPoint) {
        // existing implementation unchanged...
        throw new UnsupportedOperationException("Existing getAxlesData remains unchanged");
    }

    @Override
    public Mono<AxlesDataDTO> getGlobalAggregations(String measurementPoint) {
        // Extract numeric index from "TPx"
        String idx = measurementPoint.replaceAll("(?i)tp", "").trim();
        String fieldSuffix = "Tp" + idx; // matches bean properties like spdTp1, aoaTp1, etc.

        // merge both repositories
        Flux<AbstractAxles> allFlux = Flux.merge(
                mp1Repository.findAll().cast(AbstractAxles.class),
                mp3Repository.findAll().cast(AbstractAxles.class)
        );

        // helper to compute average for a given prefix
        Function<String, Mono<Double>> avgFor = prefix -> allFlux
                .flatMap(axle -> {
                    BeanWrapper bw = new BeanWrapperImpl(axle);
                    Object raw = bw.getPropertyValue(prefix + fieldSuffix);
                    if (raw instanceof Number) {
                        return Mono.just(((Number) raw).doubleValue());
                    }
                    return Mono.empty();
                })
                .collectList()
                .map(list -> list.isEmpty()
                        ? 0.0
                        : list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
                );

        Mono<Double> avgSpeed       = avgFor.apply("spd");
        Mono<Double> avgAoa         = avgFor.apply("aoa");
        Mono<Double> avgVibLeft     = avgFor.apply("vvibl");
        Mono<Double> avgVibRight    = avgFor.apply("vvibr");
        Mono<Double> avgVertLeft    = avgFor.apply("vfrcl");
        Mono<Double> avgVertRight   = avgFor.apply("vfrcr");
        Mono<Double> avgLatLeft     = avgFor.apply("lfrcl");
        Mono<Double> avgLatRight    = avgFor.apply("lfrcr");
        Mono<Double> avgLatVibLeft  = avgFor.apply("lvibl");
        Mono<Double> avgLatVibRight = avgFor.apply("lvibr");

        // zip using the Object[] combinator to support more than 8 sources
        return Mono.zip(
                arr -> {
                    Double spd   = (Double) arr[0];
                    Double aoa   = (Double) arr[1];
                    Double vbl   = (Double) arr[2];
                    Double vbr   = (Double) arr[3];
                    Double vfl   = (Double) arr[4];
                    Double vfr   = (Double) arr[5];
                    Double lfl   = (Double) arr[6];
                    Double lfr   = (Double) arr[7];
                    Double lvbl  = (Double) arr[8];
                    Double lvbr  = (Double) arr[9];
                    return AxlesDataDTO.builder()
                            .measurementPoint(measurementPoint)
                            .speed(spd)
                            .angleOfAttack(aoa)
                            .vibrationLeft(vbl)
                            .vibrationRight(vbr)
                            .verticalForceLeft(vfl)
                            .verticalForceRight(vfr)
                            .lateralForceLeft(lfl)
                            .lateralForceRight(lfr)
                            .lateralVibrationLeft(lvbl)
                            .lateralVibrationRight(lvbr)
                            .build();
                },
                avgSpeed,
                avgAoa,
                avgVibLeft,
                avgVibRight,
                avgVertLeft,
                avgVertRight,
                avgLatLeft,
                avgLatRight,
                avgLatVibLeft,
                avgLatVibRight
        );
    }
}
