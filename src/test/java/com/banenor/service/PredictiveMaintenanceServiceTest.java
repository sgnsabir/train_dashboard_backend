package com.banenor.service;

import com.banenor.ai.AIPredictiveMaintenanceService;
import com.banenor.config.MaintenanceProperties;
import com.banenor.dto.SensorMetricsDTO;
import com.banenor.events.MaintenanceRiskEvent;
import com.banenor.strategy.RiskCalculationStrategy;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@TestPropertySource(locations = "classpath:test.properties")
@DisplayName("PredictiveMaintenanceService Tests")
class PredictiveMaintenanceServiceTest {

    private static final String DEFAULT_ALERT_EMAIL = "alerts@example.com";

    private DashboardService dashboardService;
    private MaintenanceProperties maintenanceProperties;
    private RiskCalculationStrategy riskCalculationStrategy;
    private AIPredictiveMaintenanceService aiPredictiveMaintenanceService;
    private KafkaTemplate<String, MaintenanceRiskEvent> kafkaTemplate;
    private PredictiveMaintenanceService predictiveMaintenanceService;
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        dashboardService = mock(DashboardService.class);
        maintenanceProperties = new MaintenanceProperties();
        // Set thresholds for testing
        maintenanceProperties.setSpeedThreshold(80.0);
        maintenanceProperties.setAoaThreshold(5.0);
        maintenanceProperties.setVibrationThreshold(3.0);
        maintenanceProperties.setVerticalForceThreshold(500.0);
        maintenanceProperties.setLateralForceThreshold(300.0);
        maintenanceProperties.setLateralVibrationThreshold(2.0);
        maintenanceProperties.setAxleLoadThreshold(1000.0);
        maintenanceProperties.setWeightSpeed(0.20);
        maintenanceProperties.setWeightAoa(0.10);
        maintenanceProperties.setWeightVibration(0.10);
        maintenanceProperties.setWeightVerticalForce(0.10);
        maintenanceProperties.setWeightLateralForce(0.10);
        maintenanceProperties.setWeightLateralVibration(0.10);
        maintenanceProperties.setWeightAxleLoad(0.10);
        maintenanceProperties.setRiskScoreThreshold(0.7);

        riskCalculationStrategy = mock(RiskCalculationStrategy.class);
        aiPredictiveMaintenanceService = mock(AIPredictiveMaintenanceService.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, MaintenanceRiskEvent> kafkaTemplateMock =
                (KafkaTemplate<String, MaintenanceRiskEvent>) mock(KafkaTemplate.class);
        kafkaTemplate = kafkaTemplateMock;

        // Instantiate CircuitBreakerRegistry with defaults.
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

        predictiveMaintenanceService = new PredictiveMaintenanceServiceImpl(
                dashboardService,
                maintenanceProperties,
                riskCalculationStrategy,
                aiPredictiveMaintenanceService,
                kafkaTemplate,
                circuitBreakerRegistry
        );
    }

    @Nested
    @DisplayName("Risk Event Publishing")
    class RiskEventPublishingTests {

        @Test
        @DisplayName("Should publish risk event when calculated risk score is above threshold")
        void testGetMaintenanceAnalysis_PublishesRiskEvent_WhenAboveThreshold() {
            int analysisId = 1;
            SensorMetricsDTO metrics = new SensorMetricsDTO();
            metrics.setAverageSpeed(90.0);
            metrics.setSpeedVariance(4.0);
            metrics.setAverageAoa(6.0);
            when(dashboardService.getLatestMetrics(analysisId)).thenReturn(Mono.just(metrics));
            when(riskCalculationStrategy.calculateRisk(metrics, maintenanceProperties)).thenReturn(0.75);
            when(aiPredictiveMaintenanceService.getPrediction(metrics)).thenReturn(Mono.just("Model not configured"));

            StepVerifier.create(predictiveMaintenanceService.getMaintenanceAnalysis(analysisId, DEFAULT_ALERT_EMAIL))
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getRiskScore()).isEqualTo(0.75);
                        assertThat(response.getPredictionMessage()).contains("Model not configured");
                    })
                    .verifyComplete();

            // Verify that an event is published if riskScore >= threshold.
            ArgumentCaptor<MaintenanceRiskEvent> eventCaptor = ArgumentCaptor.forClass(MaintenanceRiskEvent.class);
            verify(kafkaTemplate, times(1)).send(eq("maintenance-risk-events"), eventCaptor.capture());
            MaintenanceRiskEvent event = eventCaptor.getValue();
            assertThat(event.getAnalysisId()).isEqualTo(analysisId);
            assertThat(event.getRiskScore()).isEqualTo(0.75);
            assertThat(event.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should not publish risk event when calculated risk score is below threshold")
        void testGetMaintenanceAnalysis_NoRiskEvent_WhenBelowThreshold() {
            int analysisId = 2;
            SensorMetricsDTO metrics = new SensorMetricsDTO();
            metrics.setAverageSpeed(75.0);
            metrics.setSpeedVariance(3.0);
            metrics.setAverageAoa(4.5);
            when(dashboardService.getLatestMetrics(analysisId)).thenReturn(Mono.just(metrics));
            when(riskCalculationStrategy.calculateRisk(metrics, maintenanceProperties)).thenReturn(0.65);
            when(aiPredictiveMaintenanceService.getPrediction(metrics)).thenReturn(Mono.just("Model not configured"));

            StepVerifier.create(predictiveMaintenanceService.getMaintenanceAnalysis(analysisId, DEFAULT_ALERT_EMAIL))
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getRiskScore()).isEqualTo(0.65);
                        assertThat(response.getPredictionMessage()).contains("Model not configured");
                    })
                    .verifyComplete();

            verify(kafkaTemplate, never()).send(anyString(), any(MaintenanceRiskEvent.class));
        }
    }
}
