package com.banenor.service;

import com.banenor.dto.HistoricalDataResponse;
import com.banenor.dto.SensorMetricsDTO;
import com.banenor.model.HaugfjellMP1Header;
import com.banenor.model.HaugfjellMP3Header;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.repository.HaugfjellMP3HeaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

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
        // Combine header lookups from both repositories using Flux.concat(...).next()
        return Flux.concat(
                        mp1HeaderRepository.findById(analysisId),
                        mp3HeaderRepository.findById(analysisId)
                )
                .next()
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No header found for train_no: " + analysisId)))
                .flatMap(header -> {
                    if (header instanceof HaugfjellMP1Header) {
                        log.info("Found MP1 header for train_no: {}", analysisId);
                        return aggregateMetricsForMP1(analysisId);
                    } else if (header instanceof HaugfjellMP3Header) {
                        log.info("Found MP3 header for train_no: {}", analysisId);
                        return aggregateMetricsForMP3(analysisId);
                    } else {
                        return Mono.error(new IllegalArgumentException("Unknown header type for train_no: " + analysisId));
                    }
                });
    }

    private Mono<SensorMetricsDTO> aggregateMetricsForMP1(Integer analysisId) {
        // Compute dynamic aggregations using the new queries (grouped by 'vit')
        Mono<Double> avgSpeed = mp1Repository.findDynamicSpeedAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgSpeed())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> avgSpeedSquare = mp1Repository.findDynamicSpeedAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgSquareSpeed())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> speedVariance = avgSpeed.zipWith(avgSpeedSquare, (avg, avgSq) -> avgSq - (avg * avg));

        Mono<Double> avgAoa = mp1Repository.findDynamicAngleAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgAoa())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> avgVibrationLeft = mp1Repository.findDynamicVibrationLeftAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgVibrationLeft())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> avgVibrationRight = mp1Repository.findDynamicVibrationRightAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgVibrationRight())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> avgVerticalForceLeft = mp1Repository.findDynamicVerticalForceLeftAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgVerticalForceLeft())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> avgVerticalForceRight = mp1Repository.findDynamicVerticalForceRightAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgVerticalForceRight())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> avgLateralForceLeft = mp1Repository.findDynamicLateralForceLeftAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgLateralForceLeft())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> avgLateralForceRight = mp1Repository.findDynamicLateralForceRightAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgLateralForceRight())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> avgLateralVibrationLeft = mp1Repository.findDynamicLateralVibrationLeftAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgLateralVibrationLeft())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> avgLateralVibrationRight = mp1Repository.findDynamicLateralVibrationRightAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgLateralVibrationRight())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        // Legacy global queries for axle loads remain unchanged
        Mono<Double> avgAxleLoadLeft = mp1Repository.findGlobalAverageAxleLoadLeft().defaultIfEmpty(0.0);
        Mono<Double> avgAxleLoadRight = mp1Repository.findGlobalAverageAxleLoadRight().defaultIfEmpty(0.0);

        List<Mono<Double>> sources = List.of(
                avgSpeed, speedVariance, avgAoa, avgVibrationLeft, avgVibrationRight,
                avgVerticalForceLeft, avgVerticalForceRight, avgLateralForceLeft, avgLateralForceRight,
                avgLateralVibrationLeft, avgLateralVibrationRight, avgAxleLoadLeft, avgAxleLoadRight
        );

        return Mono.zip(sources, results -> {
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
            dto.setAverageAxleLoadLeft((Double) results[11]);
            dto.setAverageAxleLoadRight((Double) results[12]);
            dto.setAnalysisId(analysisId);
            return dto;
        });
    }

    private Mono<SensorMetricsDTO> aggregateMetricsForMP3(Integer analysisId) {
        // Dynamic aggregations for MP3 using the same approach as MP1
        Mono<Double> avgSpeed = mp3Repository.findDynamicSpeedAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgSpeed())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        Mono<Double> avgSpeedSquare = mp3Repository.findDynamicSpeedAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgSquareSpeed())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        Mono<Double> speedVariance = avgSpeed.zipWith(avgSpeedSquare, (avg, avgSq) -> avgSq - (avg * avg));

        Mono<Double> avgAoa = mp3Repository.findDynamicAngleAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgAoa())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> avgVibrationLeft = mp3Repository.findDynamicVibrationLeftAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgVibrationLeft())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        Mono<Double> avgVibrationRight = mp3Repository.findDynamicVibrationRightAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgVibrationRight())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> avgVerticalForceLeft = mp3Repository.findDynamicVerticalForceLeftAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgVerticalForceLeft())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        Mono<Double> avgVerticalForceRight = mp3Repository.findDynamicVerticalForceRightAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgVerticalForceRight())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> avgLateralForceLeft = mp3Repository.findDynamicLateralForceLeftAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgLateralForceLeft())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        Mono<Double> avgLateralForceRight = mp3Repository.findDynamicLateralForceRightAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgLateralForceRight())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> avgLateralVibrationLeft = mp3Repository.findDynamicLateralVibrationLeftAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgLateralVibrationLeft())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        Mono<Double> avgLateralVibrationRight = mp3Repository.findDynamicLateralVibrationRightAggregationsByTrainNo(analysisId)
                .map(agg -> agg.getAvgLateralVibrationRight())
                .collectList()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        Mono<Double> avgAxleLoadLeft = mp3Repository.findGlobalAverageAxleLoadLeft().defaultIfEmpty(0.0);
        Mono<Double> avgAxleLoadRight = mp3Repository.findGlobalAverageAxleLoadRight().defaultIfEmpty(0.0);

        List<Mono<Double>> sources = List.of(
                avgSpeed, speedVariance, avgAoa, avgVibrationLeft, avgVibrationRight,
                avgVerticalForceLeft, avgVerticalForceRight, avgLateralForceLeft, avgLateralForceRight,
                avgLateralVibrationLeft, avgLateralVibrationRight, avgAxleLoadLeft, avgAxleLoadRight
        );

        return Mono.zip(sources, results -> {
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
            dto.setAverageAxleLoadLeft((Double) results[11]);
            dto.setAverageAxleLoadRight((Double) results[12]);
            dto.setAnalysisId(analysisId);
            return dto;
        });
    }

    @Override
    public Mono<HistoricalDataResponse> getHistoricalData(Integer analysisId) {
        return getLatestMetrics(analysisId)
                .map(latestMetrics -> {
                    HistoricalDataResponse response = new HistoricalDataResponse();
                    response.setAnalysisId(analysisId);
                    response.setMetricsHistory(java.util.Collections.singletonList(latestMetrics));
                    response.setPage(1);
                    response.setSize(1);
                    response.setTotalRecords(1L);
                    return response;
                });
    }
}
