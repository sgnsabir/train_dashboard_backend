package com.banenor.integration;

import com.banenor.model.HaugfjellMP1Axles;
import com.banenor.model.HaugfjellMP1Header;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.service.AggregationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(properties = {
        "spring.config.location=classpath:test.properties",
        "spring.autoconfigure.exclude=" // Override any exclusions so that security beans are loaded
})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
public class AggregationIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private HaugfjellMP1HeaderRepository headerRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private HaugfjellMP1AxlesRepository axlesRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private AggregationService aggregationService;

    private HaugfjellMP1Header testHeader;

    @BeforeEach
    void setUp() {
        // Clean repositories before each test.
        axlesRepository.deleteAll().block();
        headerRepository.deleteAll().block();

        // Create and persist a header record for station "MP1" with an explicit train_no.
        testHeader = HaugfjellMP1Header.builder()
                .trainNo(1001) // Explicitly set primary key (train_no)
                .mplace("MP1")
                .mstation("Haugfjell")
                .cooLat(60.0)
                .cooLong(10.0)
                .mstartTime(Timestamp.valueOf(LocalDateTime.now().minusMinutes(10)))
                .mstopTime(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        testHeader = headerRepository.save(testHeader).block();
        log.info("Persisted header with train_no: {}", testHeader.getTrainNo());

        // Create two axle records with known speed values for aggregation testing.
        HaugfjellMP1Axles axle1 = HaugfjellMP1Axles.builder()
                .header(testHeader)
                .spdTp1(80.0)
                .build();
        HaugfjellMP1Axles axle2 = HaugfjellMP1Axles.builder()
                .header(testHeader)
                .spdTp1(100.0)
                .build();
        axlesRepository.save(axle1).block();
        axlesRepository.save(axle2).block();
    }

    @Test
    void testAverageSpeedAggregation() {
        Double avgSpeed = aggregationService.getAverageSpeed(testHeader.getTrainNo()).block();
        // Expected average: (80 + 100) / 2 = 90.0
        assertThat(avgSpeed).isEqualTo(90.0);
    }

    @Test
    void testSpeedVarianceAggregation() {
        Double variance = aggregationService.getSpeedVariance(testHeader.getTrainNo()).block();
        // Expected variance: avg = 90, avgSquare = ((80^2 + 100^2)/2) = 8200, variance = 8200 - (90^2 = 8100) = 100
        assertThat(variance).isEqualTo(100.0);
    }
}
