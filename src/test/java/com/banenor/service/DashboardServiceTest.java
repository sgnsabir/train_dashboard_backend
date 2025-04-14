package com.banenor.service;

import com.banenor.dto.SensorMetricsDTO;
import com.banenor.model.HaugfjellMP1Header;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.repository.HaugfjellMP3HeaderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestPropertySource(locations = "classpath:test.properties")
class DashboardServiceTest {

    private HaugfjellMP1HeaderRepository mp1HeaderRepository;
    private HaugfjellMP3HeaderRepository mp3HeaderRepository;
    private HaugfjellMP1AxlesRepository mp1AxlesRepository;
    private HaugfjellMP3AxlesRepository mp3AxlesRepository;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        mp1HeaderRepository = mock(HaugfjellMP1HeaderRepository.class);
        mp3HeaderRepository = mock(HaugfjellMP3HeaderRepository.class);
        mp1AxlesRepository = mock(HaugfjellMP1AxlesRepository.class);
        mp3AxlesRepository = mock(HaugfjellMP3AxlesRepository.class);
        dashboardService = new DashboardServiceImpl(mp1HeaderRepository, mp3HeaderRepository, mp1AxlesRepository, mp3AxlesRepository);
    }

    @Test
    void testGetLatestMetrics_MP1() {
        int trainNo = 1;
        // Simulate that an MP1 header exists.
        HaugfjellMP1Header header = new HaugfjellMP1Header();
        when(mp1HeaderRepository.findById(trainNo)).thenReturn(Mono.just(header));

        // Stub all relevant repository methods from MP1 repository.
        when(mp1AxlesRepository.findAverageSpeedByTrainNo(trainNo)).thenReturn(Mono.just(80.0));
        when(mp1AxlesRepository.findSpeedVarianceByTrainNo(trainNo)).thenReturn(Mono.just(4.0));
        when(mp1AxlesRepository.findAverageAoaByTrainNo(trainNo)).thenReturn(Mono.just(5.0));
        when(mp1AxlesRepository.findAverageVibrationLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageVibrationRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageVerticalForceLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageVerticalForceRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageLateralForceLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageLateralForceRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageLateralVibrationLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageLateralVibrationRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageAxleLoadLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageAxleLoadRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));

        StepVerifier.create(dashboardService.getLatestMetrics(trainNo))
                .assertNext(metrics -> {
                    assertNotNull(metrics, "Metrics should not be null");
                    assertEquals(80.0, metrics.getAverageSpeed());
                    assertEquals(4.0, metrics.getSpeedVariance());
                    assertEquals(5.0, metrics.getAverageAoa());
                    // Other fields default to 0.0
                    assertEquals(0.0, metrics.getAverageVibrationLeft());
                    assertEquals(0.0, metrics.getAverageVibrationRight());
                    assertEquals(0.0, metrics.getAverageVerticalForceLeft());
                    assertEquals(0.0, metrics.getAverageVerticalForceRight());
                    assertEquals(0.0, metrics.getAverageLateralForceLeft());
                    assertEquals(0.0, metrics.getAverageLateralForceRight());
                    assertEquals(0.0, metrics.getAverageLateralVibrationLeft());
                    assertEquals(0.0, metrics.getAverageLateralVibrationRight());
                    assertEquals(0.0, metrics.getAverageAxleLoadLeft());
                    assertEquals(0.0, metrics.getAverageAxleLoadRight());
                })
                .verifyComplete();
    }

    @Test
    void testGetLatestMetrics_MP3() {
        int trainNo = 2;
        // Simulate that an MP3 header exists but no MP1 header.
        when(mp1HeaderRepository.findById(trainNo)).thenReturn(Mono.empty());
        // Simulate MP3 header exists.
        when(mp3HeaderRepository.findById(trainNo)).thenReturn(Mono.just(new com.banenor.model.HaugfjellMP3Header()));

        // Stub all relevant repository methods from MP3 repository.
        when(mp3AxlesRepository.findAverageSpeedByTrainNo(trainNo)).thenReturn(Mono.just(85.0));
        when(mp3AxlesRepository.findSpeedVarianceByTrainNo(trainNo)).thenReturn(Mono.just(6.0));
        when(mp3AxlesRepository.findAverageAoaByTrainNo(trainNo)).thenReturn(Mono.just(6.0));
        when(mp3AxlesRepository.findAverageVibrationLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp3AxlesRepository.findAverageVibrationRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp3AxlesRepository.findAverageVerticalForceLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp3AxlesRepository.findAverageVerticalForceRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp3AxlesRepository.findAverageLateralForceLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp3AxlesRepository.findAverageLateralForceRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp3AxlesRepository.findAverageLateralVibrationLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp3AxlesRepository.findAverageLateralVibrationRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp3AxlesRepository.findAverageAxleLoadLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp3AxlesRepository.findAverageAxleLoadRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));

        StepVerifier.create(dashboardService.getLatestMetrics(trainNo))
                .assertNext(metrics -> {
                    assertNotNull(metrics, "Metrics should not be null for MP3 header");
                    assertEquals(85.0, metrics.getAverageSpeed());
                    assertEquals(6.0, metrics.getSpeedVariance());
                    assertEquals(6.0, metrics.getAverageAoa());
                    // Other fields default to 0.0
                    assertEquals(0.0, metrics.getAverageVibrationLeft());
                    assertEquals(0.0, metrics.getAverageVibrationRight());
                    assertEquals(0.0, metrics.getAverageVerticalForceLeft());
                    assertEquals(0.0, metrics.getAverageVerticalForceRight());
                    assertEquals(0.0, metrics.getAverageLateralForceLeft());
                    assertEquals(0.0, metrics.getAverageLateralForceRight());
                    assertEquals(0.0, metrics.getAverageLateralVibrationLeft());
                    assertEquals(0.0, metrics.getAverageLateralVibrationRight());
                    assertEquals(0.0, metrics.getAverageAxleLoadLeft());
                    assertEquals(0.0, metrics.getAverageAxleLoadRight());
                })
                .verifyComplete();
    }

    @Test
    void testGetLatestMetrics_NoHeaderFound() {
        int trainNo = 999;
        when(mp1HeaderRepository.findById(trainNo)).thenReturn(Mono.empty());
        when(mp3HeaderRepository.findById(trainNo)).thenReturn(Mono.empty());

        StepVerifier.create(dashboardService.getLatestMetrics(trainNo))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().contains("No header found"))
                .verify();
    }

    @Test
    void testGetHistoricalData() {
        int trainNo = 1;
        // For historical data, we assume the method returns a single snapshot.
        HaugfjellMP1Header header = new HaugfjellMP1Header();
        when(mp1HeaderRepository.findById(trainNo)).thenReturn(Mono.just(header));
        // Stub all required repository methods with non-null Monos.
        when(mp1AxlesRepository.findAverageSpeedByTrainNo(trainNo)).thenReturn(Mono.just(80.0));
        when(mp1AxlesRepository.findSpeedVarianceByTrainNo(trainNo)).thenReturn(Mono.just(4.0));
        when(mp1AxlesRepository.findAverageAoaByTrainNo(trainNo)).thenReturn(Mono.just(5.0));
        when(mp1AxlesRepository.findAverageVibrationLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageVibrationRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageVerticalForceLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageVerticalForceRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageLateralForceLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageLateralForceRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageLateralVibrationLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageLateralVibrationRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageAxleLoadLeftByTrainNo(trainNo)).thenReturn(Mono.just(0.0));
        when(mp1AxlesRepository.findAverageAxleLoadRightByTrainNo(trainNo)).thenReturn(Mono.just(0.0));

        StepVerifier.create(dashboardService.getHistoricalData(trainNo))
                .assertNext(response -> {
                    assertNotNull(response, "Historical data response should not be null");
                    assertEquals(trainNo, response.getAnalysisId());
                    assertEquals(1, response.getMetricsHistory().size(), "Historical data should contain one snapshot");
                    SensorMetricsDTO snapshot = response.getMetricsHistory().getFirst();
                    assertEquals(80.0, snapshot.getAverageSpeed());
                    assertEquals(4.0, snapshot.getSpeedVariance());
                    assertEquals(5.0, snapshot.getAverageAoa());
                })
                .verifyComplete();
    }
}
