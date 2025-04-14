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
                        return aggregateMetricsForMP1(analysisId, mp1Repository);
                    } else if (header instanceof HaugfjellMP3Header) {
                        log.info("Found MP3 header for train_no: {}", analysisId);
                        return aggregateMetricsForMP3(analysisId, mp3Repository);
                    } else {
                        return Mono.error(new IllegalArgumentException("Unknown header type for train_no: " + analysisId));
                    }
                });
    }

    private Mono<SensorMetricsDTO> aggregateMetricsForMP1(Integer analysisId, HaugfjellMP1AxlesRepository repo) {
        return Mono.zip(
                List.of(
                        repo.findAverageSpeedByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findSpeedVarianceByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageAoaByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageVibrationLeftByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageVibrationRightByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageVerticalForceLeftByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageVerticalForceRightByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageLateralForceLeftByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageLateralForceRightByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageLateralVibrationLeftByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageLateralVibrationRightByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageAxleLoadLeftByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageAxleLoadRightByTrainNo(analysisId).defaultIfEmpty(0.0)
                ),
                results -> {
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
                }
        );
    }

    private Mono aggregateMetricsForMP3(Integer analysisId, HaugfjellMP3AxlesRepository repo) {
        return Mono.zip(
                List.of(
                        repo.findAverageSpeedByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findSpeedVarianceByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageAoaByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageVibrationLeftByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageVibrationRightByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageVerticalForceLeftByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageVerticalForceRightByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageLateralForceLeftByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageLateralForceRightByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageLateralVibrationLeftByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageLateralVibrationRightByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageAxleLoadLeftByTrainNo(analysisId).defaultIfEmpty(0.0),
                        repo.findAverageAxleLoadRightByTrainNo(analysisId).defaultIfEmpty(0.0)
                ),
                results -> {
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
                }
        );
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
