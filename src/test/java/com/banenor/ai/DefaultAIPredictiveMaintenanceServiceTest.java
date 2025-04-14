package com.banenor.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.banenor.dto.SensorMetricsDTO;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Updated tests for DefaultAIPredictiveMaintenanceService.
 * Uses a stub ExchangeFunction to simulate an external AI model service.
 */
@DisplayName("DefaultAIPredictiveMaintenanceService Tests")
class DefaultAIPredictiveMaintenanceServiceTest {

    /**
     * StubExchangeFunction returns a fixed ClientResponse with the body "Model not configured".
     */
    private static class StubExchangeFunction implements ExchangeFunction {
        @Override
        @NonNull
        public Mono<ClientResponse> exchange(ClientRequest request) {
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "text/plain")
                    .body("Model not configured")
                    .build());
        }
    }

    // Create a WebClient.Builder with our stub ExchangeFunction.
    private final WebClient.Builder webClientBuilder = WebClient.builder()
            .exchangeFunction(new StubExchangeFunction());

    // Instantiate the service with a dummy URL and our WebClient builder.
    private final AIPredictiveMaintenanceService service =
            new DefaultAIPredictiveMaintenanceService("http://dummy-ai-service", webClientBuilder);

    @Test
    @DisplayName("When given empty metrics, return stub prediction")
    void testGetPrediction_WithEmptyMetrics() {
        SensorMetricsDTO metrics = new SensorMetricsDTO();
        StepVerifier.create(service.getPrediction(metrics))
                .expectNextMatches(prediction -> prediction != null && prediction.contains("Model not configured"))
                .verifyComplete();
    }

    @Test
    @DisplayName("When given populated metrics, still return stub prediction")
    void testGetPrediction_WithPopulatedMetrics() {
        SensorMetricsDTO metrics = new SensorMetricsDTO();
        metrics.setAverageSpeed(90.0);
        metrics.setSpeedVariance(4.0);
        metrics.setAverageAoa(5.5);
        metrics.setAverageVibrationLeft(3.0);
        metrics.setAverageVibrationRight(3.0);
        metrics.setAverageVerticalForceLeft(550.0);
        metrics.setAverageVerticalForceRight(550.0);
        metrics.setAverageLateralForceLeft(320.0);
        metrics.setAverageLateralForceRight(320.0);
        metrics.setAverageLateralVibrationLeft(2.5);
        metrics.setAverageLateralVibrationRight(2.5);
        metrics.setAverageAxleLoadLeft(1050.0);
        StepVerifier.create(service.getPrediction(metrics))
                .expectNextMatches(prediction -> prediction != null && prediction.contains("Model not configured"))
                .verifyComplete();
    }

    @Test
    @DisplayName("When given null metrics, return stub prediction without error")
    void testGetPrediction_WithNullMetrics() {
        StepVerifier.create(service.getPrediction(null))
                .expectNextMatches(prediction -> prediction != null && prediction.contains("Model not configured"))
                .verifyComplete();
    }

    @Nested
    @DisplayName("Various Sensor Metrics Scenarios")
    class MetricsScenarios {

        @Test
        @DisplayName("Low-risk sensor metrics scenario")
        void testGetPrediction_LowRiskMetrics() {
            SensorMetricsDTO metrics = new SensorMetricsDTO();
            metrics.setAverageSpeed(70.0);
            metrics.setAverageAoa(3.0);
            StepVerifier.create(service.getPrediction(metrics))
                    .expectNextMatches(prediction -> prediction != null && prediction.contains("Model not configured"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("High-risk sensor metrics scenario")
        void testGetPrediction_HighRiskMetrics() {
            SensorMetricsDTO metrics = new SensorMetricsDTO();
            metrics.setAverageSpeed(120.0);
            metrics.setAverageAoa(8.0);
            StepVerifier.create(service.getPrediction(metrics))
                    .expectNextMatches(prediction -> prediction != null && prediction.contains("Model not configured"))
                    .verifyComplete();
        }
    }
}
