package com.banenor.ai;

import com.banenor.dto.SensorMetricsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class DefaultAIPredictiveMaintenanceService implements AIPredictiveMaintenanceService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAIPredictiveMaintenanceService.class);

    // Base URL for the external AI model service. This should be configured externally.
    private final String aiServiceUrl;

    private final WebClient webClient;

    public DefaultAIPredictiveMaintenanceService(
            @Value("${ai.service.url:http://ai-model-service/api/predict}") String aiServiceUrl,
            WebClient.Builder webClientBuilder) {
        this.aiServiceUrl = aiServiceUrl;
        this.webClient = webClientBuilder
                .baseUrl(this.aiServiceUrl)
                .build();
    }

    /**
     * Calls an external AI service to get a prediction based on aggregated sensor metrics.
     * This implementation uses a reactive WebClient to perform a POST call and applies a timeout and retry strategy.
     *
     * @param metrics Aggregated sensor metrics.
     * @return A Mono emitting the AI prediction message.
     */
    @Override
    public Mono<String> getPrediction(SensorMetricsDTO metrics) {
        return webClient.post()
                .uri("") // Using the base URL; adjust URI path if needed.
                .bodyValue(metrics)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientResponseException))
                .doOnNext(response -> logger.info("Received AI prediction response: {}", response))
                .doOnError(error -> logger.error("Error while calling AI prediction service: {}", error.getMessage()))
                .onErrorResume(error -> {
                    // Fallback to a default message if the AI service is unavailable or fails.
                    return Mono.just("AI prediction service unavailable. Please try again later.");
                });
    }
}
