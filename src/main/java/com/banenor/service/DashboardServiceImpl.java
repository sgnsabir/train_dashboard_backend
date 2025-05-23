package com.banenor.service;

import com.banenor.dto.AlertDTO;
import com.banenor.dto.HistoricalDataResponse;
import com.banenor.dto.SensorMetricsDTO;
import com.banenor.dto.SystemDashboardDTO;
import com.banenor.model.AbstractAxles;
import com.banenor.model.HaugfjellMP1Header;
import com.banenor.model.HaugfjellMP3Header;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.repository.HaugfjellMP3HeaderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.beans.PropertyDescriptor;
import java.time.Duration;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final HaugfjellMP1HeaderRepository mp1HeaderRepository;
    private final HaugfjellMP3HeaderRepository mp3HeaderRepository;
    private final HaugfjellMP1AxlesRepository mp1Repository;
    private final HaugfjellMP3AxlesRepository mp3Repository;
    private final AlertService alertService;
    private final SystemHealthService systemHealthService;

    @Override
    public Mono<SensorMetricsDTO> getLatestMetrics(Integer analysisId) {
        return Flux.concat(
                        mp1HeaderRepository.findById(analysisId),
                        mp3HeaderRepository.findById(analysisId)
                )
                .next()
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("No header for analysisId=" + analysisId)))
                .flatMap(header -> {
                    boolean isMp1 = header instanceof HaugfjellMP1Header;
                    log.info("Found {} header for analysisId={}",
                            isMp1 ? "MP1" : "MP3", analysisId);
                    Flux<AbstractAxles> raw = isMp1
                            ? mp1Repository.findByTrainNo(analysisId).cast(AbstractAxles.class)
                            : mp3Repository.findByTrainNo(analysisId).cast(AbstractAxles.class);
                    return aggregateMetrics(analysisId, raw);
                })
                .doOnError(e -> log.error("getLatestMetrics failed for {}: {}", analysisId, e.getMessage(), e));
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
                })
                .doOnError(e -> log.error("getHistoricalData failed for {}: {}", analysisId, e.getMessage(), e));
    }

    @Override
    public Mono<SystemDashboardDTO> getSystemDashboard() {
        Flux<Integer> allIds = Flux.concat(
                        mp1HeaderRepository.findAll().map(HaugfjellMP1Header::getTrainNo),
                        mp3HeaderRepository.findAll().map(HaugfjellMP3Header::getTrainNo)
                )
                .distinct();

        Mono<SensorMetricsDTO> globalMetrics = allIds
                .flatMap(this::getLatestMetrics)
                .collectList()
                .map(this::aggregateAcross)
                .doOnError(e -> log.error("aggregateAcross failed: {}", e.getMessage(), e));

        Mono<List<AlertDTO>> alerts = alertService.getAlertHistory(null, null, null)
                .map(r -> AlertDTO.builder()
                        .id(r.getId())
                        .subject(r.getSubject())
                        .message(r.getMessage())
                        .timestamp(r.getTimestamp())
                        .severity(r.getSeverity())
                        .trainNo(r.getTrainNo())
                        .acknowledged(r.getAcknowledged())
                        .acknowledgedBy(r.getAcknowledgedBy())
                        .build())
                .collectList()
                .doOnError(e -> log.error("fetch alerts failed: {}", e.getMessage(), e));

        Mono<String> status = systemHealthService.getSystemStatus()
                .doOnError(e -> log.error("fetch status failed: {}", e.getMessage(), e));

        return Mono.zip(globalMetrics, alerts, status)
                .map(tuple -> new SystemDashboardDTO(
                        tuple.getT1(),
                        tuple.getT2(),
                        tuple.getT3()
                ))
                .doOnError(e -> log.error("getSystemDashboard failed: {}", e.getMessage(), e));
    }

    private SensorMetricsDTO aggregateAcross(List<SensorMetricsDTO> list) {
        SensorMetricsDTO agg = new SensorMetricsDTO();
        agg.setAverageSpeed(mean(list, SensorMetricsDTO::getAverageSpeed));
        agg.setSpeedVariance(mean(list, SensorMetricsDTO::getSpeedVariance));
        agg.setAverageAoa(mean(list, SensorMetricsDTO::getAverageAoa));
        agg.setAverageVibrationLeft(mean(list, SensorMetricsDTO::getAverageVibrationLeft));
        agg.setAverageVibrationRight(mean(list, SensorMetricsDTO::getAverageVibrationRight));
        agg.setAverageVerticalForceLeft(mean(list, SensorMetricsDTO::getAverageVerticalForceLeft));
        agg.setAverageVerticalForceRight(mean(list, SensorMetricsDTO::getAverageVerticalForceRight));
        agg.setAverageLateralForceLeft(mean(list, SensorMetricsDTO::getAverageLateralForceLeft));
        agg.setAverageLateralForceRight(mean(list, SensorMetricsDTO::getAverageLateralForceRight));
        agg.setAverageLateralVibrationLeft(mean(list, SensorMetricsDTO::getAverageLateralVibrationLeft));
        agg.setAverageLateralVibrationRight(mean(list, SensorMetricsDTO::getAverageLateralVibrationRight));
        return agg;
    }

    private double mean(List<SensorMetricsDTO> list,
                        java.util.function.ToDoubleFunction<SensorMetricsDTO> fn) {
        DoubleSummaryStatistics stats = list.stream()
                .mapToDouble(fn)
                .summaryStatistics();
        return stats.getCount() > 0 ? stats.getAverage() : 0.0;
    }

    private Mono<SensorMetricsDTO> aggregateMetrics(Integer analysisId,
                                                    Flux<AbstractAxles> rawFlux) {
        Flux<AbstractAxles> shared = rawFlux.publish().refCount(1);

        Mono<Double> avgSpeed     = average(valuesFor(shared, n -> n.matches("spdTp\\d+")));
        Mono<Double> speedVar     = average(valuesFor(shared, n -> n.matches("spdTp\\d+")))
                .zipWith(
                        average(valuesFor(shared, n -> n.matches("spdTp\\d+")).map(v -> v * v)),
                        (avg, avgSq) -> avgSq - avg * avg
                );
        Mono<Double> avgAoa       = average(valuesFor(shared, n -> n.matches("aoaTp\\d+")));
        Mono<Double> avgVibL      = average(valuesFor(shared, n -> n.matches("vviblTp\\d+")));
        Mono<Double> avgVibR      = average(valuesFor(shared, n -> n.matches("vvibrTp\\d+")));
        Mono<Double> avgVertL     = average(valuesFor(shared, n -> n.matches("vfrclTp\\d+")));
        Mono<Double> avgVertR     = average(valuesFor(shared, n -> n.matches("vfrcrTp\\d+")));
        Mono<Double> avgLatL      = average(valuesFor(shared, n -> n.matches("lfrclTp\\d+")));
        Mono<Double> avgLatR      = average(valuesFor(shared, n -> n.matches("lfrcrTp\\d+")));
        Mono<Double> avgLatVibL   = average(valuesFor(shared, n -> n.matches("lviblTp\\d+")));
        Mono<Double> avgLatVibR   = average(valuesFor(shared, n -> n.matches("lvibrTp\\d+")));

        List<Mono<Double>> metrics = List.of(
                avgSpeed,
                speedVar,
                avgAoa,
                avgVibL,
                avgVibR,
                avgVertL,
                avgVertR,
                avgLatL,
                avgLatR,
                avgLatVibL,
                avgLatVibR
        );

        return Mono.zip(metrics, results -> {
                    SensorMetricsDTO d = new SensorMetricsDTO();
                    d.setAnalysisId(analysisId);
                    d.setAverageSpeed((Double) results[0]);
                    d.setSpeedVariance((Double) results[1]);
                    d.setAverageAoa((Double) results[2]);
                    d.setAverageVibrationLeft((Double) results[3]);
                    d.setAverageVibrationRight((Double) results[4]);
                    d.setAverageVerticalForceLeft((Double) results[5]);
                    d.setAverageVerticalForceRight((Double) results[6]);
                    d.setAverageLateralForceLeft((Double) results[7]);
                    d.setAverageLateralForceRight((Double) results[8]);
                    d.setAverageLateralVibrationLeft((Double) results[9]);
                    d.setAverageLateralVibrationRight((Double) results[10]);
                    return d;
                })
                .timeout(Duration.ofSeconds(5))
                .doOnError(e -> log.error("aggregateMetrics failed: {}", e.getMessage(), e));
    }

    private Flux<Double> valuesFor(Flux<AbstractAxles> flux, Predicate<String> filter) {
        return flux.flatMap(axle -> {
            var bw = new BeanWrapperImpl(axle);
            List<Double> vals = Arrays.stream(bw.getPropertyDescriptors())
                    .map(PropertyDescriptor::getName)
                    .filter(filter)
                    .map(bw::getPropertyValue)
                    .filter(v -> v instanceof Number)
                    .map(v -> ((Number) v).doubleValue())
                    .collect(Collectors.toList());
            return Flux.fromIterable(vals);
        });
    }

    private Mono<Double> average(Flux<Double> flux) {
        return flux.collectList()
                .map(list -> list.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0))
                .doOnError(e -> log.error("average failed: {}", e.getMessage(), e));
    }
}
