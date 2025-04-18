package com.banenor.service;

import com.banenor.dto.HistoricalDataResponse;
import com.banenor.dto.SensorMetricsDTO;
import com.banenor.model.AbstractAxles;
import com.banenor.model.HaugfjellMP1Header;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.repository.HaugfjellMP3HeaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    private final HaugfjellMP1HeaderRepository mp1HeaderRepository;
    private final HaugfjellMP3HeaderRepository mp3HeaderRepository;
    private final HaugfjellMP1AxlesRepository mp1Repository;
    private final HaugfjellMP3AxlesRepository mp3Repository;

    public DashboardServiceImpl(HaugfjellMP1HeaderRepository mp1HeaderRepository,
                                HaugfjellMP3HeaderRepository mp3HeaderRepository,
                                HaugfjellMP1AxlesRepository mp1Repository,
                                HaugfjellMP3AxlesRepository mp3Repository) {
        this.mp1HeaderRepository = mp1HeaderRepository;
        this.mp3HeaderRepository = mp3HeaderRepository;
        this.mp1Repository = mp1Repository;
        this.mp3Repository = mp3Repository;
    }

    @Override
    public Mono<SensorMetricsDTO> getLatestMetrics(Integer analysisId) {
        return Flux.concat(
                        mp1HeaderRepository.findById(analysisId),
                        mp3HeaderRepository.findById(analysisId)
                )
                .next()
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "No header found for analysisId: " + analysisId)))
                .flatMap(header -> {
                    boolean isMp1 = header instanceof HaugfjellMP1Header;
                    log.info("Found {} header for analysisId: {}",
                            isMp1 ? "MP1" : "MP3", analysisId);
                    Flux<AbstractAxles> raw = isMp1
                            ? mp1Repository.findByTrainNo(analysisId).cast(AbstractAxles.class)
                            : mp3Repository.findByTrainNo(analysisId).cast(AbstractAxles.class);
                    return aggregateMetrics(analysisId, raw);
                });
    }

    private Mono<SensorMetricsDTO> aggregateMetrics(Integer analysisId,
                                                    Flux<AbstractAxles> rawFlux) {
        // Share the flux so we can reuse it for each metric
        Flux<AbstractAxles> shared = rawFlux.publish().refCount(1);

        Mono<Double> avgSpeed = average(valuesForPrefix(shared, name -> name.matches("spdTp\\d+")));
        Mono<Double> avgSpeedSq = average(
                valuesForPrefix(shared, name -> name.matches("spdTp\\d+"))
                        .map(v -> v * v)
        );
        Mono<Double> speedVariance = avgSpeed.zipWith(avgSpeedSq,
                (avg, avgSq) -> avgSq - (avg * avg)
        );

        Mono<Double> avgAoa = average(valuesForPrefix(shared, name -> name.matches("aoaTp\\d+")));

        Mono<Double> avgVibLeft  = average(valuesForPrefix(shared, name -> name.matches("vviblTp\\d+")));
        Mono<Double> avgVibRight = average(valuesForPrefix(shared, name -> name.matches("vvibrTp\\d+")));

        Mono<Double> avgVertLeft  = average(valuesForPrefix(shared, name -> name.matches("vfrclTp\\d+")));
        Mono<Double> avgVertRight = average(valuesForPrefix(shared, name -> name.matches("vfrcrTp\\d+")));

        Mono<Double> avgLatLeft  = average(valuesForPrefix(shared, name -> name.matches("lfrclTp\\d+")));
        Mono<Double> avgLatRight = average(valuesForPrefix(shared, name -> name.matches("lfrcrTp\\d+")));

        Mono<Double> avgLatVibLeft  = average(valuesForPrefix(shared, name -> name.matches("lviblTp\\d+")));
        Mono<Double> avgLatVibRight = average(valuesForPrefix(shared, name -> name.matches("lvibrTp\\d+")));

        List<Mono<Double>> metrics = List.of(
                avgSpeed,
                speedVariance,
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

        return Mono.zip(metrics, results -> {
            SensorMetricsDTO dto = new SensorMetricsDTO();
            dto.setAverageSpeed((Double) results[0]);
            dto.setSpeedVariance((Double) results[1]);
            dto.setAverageAoa((Double) results[2]);
            dto.setAverageVibrationLeft((Double) results[3]);
            dto.setAverageVibrationRight((Double) results[4]);
            dto.setAverageVerticalForceLeft((Double) results[5]);
            dto.setAverageVerticalForceRight((Double) results[6]);
            dto.setAverageLateralForceLeft((Double) results[7]);
            dto.setAverageLateralForceRight((Double) results[8]);
            dto.setAverageLateralVibrationLeft((Double) results[9]);
            dto.setAverageLateralVibrationRight((Double) results[10]);
            dto.setAnalysisId(analysisId);
            return dto;
        });
    }

    private Flux<Double> valuesForPrefix(Flux<AbstractAxles> raw,
                                         Predicate<String> nameFilter) {
        return raw.flatMap(axle -> {
            BeanWrapper bw = new BeanWrapperImpl(axle);
            List<Double> vals =
                    List.of(bw.getPropertyDescriptors()).stream()
                            .map(PropertyDescriptor::getName)
                            .filter(nameFilter)
                            .map(bw::getPropertyValue)
                            .filter(v -> v instanceof Number)
                            .map(v -> ((Number) v).doubleValue())
                            .collect(Collectors.toList());
            return Flux.fromIterable(vals);
        });
    }

    private Mono<Double> average(Flux<Double> flux) {
        return flux
                .collectList()
                .map(list -> list.isEmpty()
                        ? 0.0
                        : list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
    }

    @Override
    public Mono<HistoricalDataResponse> getHistoricalData(Integer analysisId) {
        return getLatestMetrics(analysisId)
                .map(latest -> {
                    HistoricalDataResponse resp = new HistoricalDataResponse();
                    resp.setAnalysisId(analysisId);
                    resp.setMetricsHistory(List.of(latest));
                    resp.setPage(1);
                    resp.setSize(1);
                    resp.setTotalRecords(1L);
                    return resp;
                });
    }
}
