package com.banenor.integration;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.banenor.dto.AnalysisHeaderDTO;
import com.banenor.dto.AnalysisMeasurementDTO;
import com.banenor.dto.SensorDataPayload;
import com.banenor.model.HaugfjellMP1Header;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@EmbeddedKafka(partitions = 1, topics = {"sensor-data-topic", "maintenance-risk-events"})
@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:test.properties",
        properties = {"spring.security.enabled=false"}
)
public class EndToEndIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper; // For JSON serialization/deserialization

    @Autowired
    private HaugfjellMP1HeaderRepository mp1HeaderRepository; // To verify persisted header records

    @Autowired
    private WebTestClient webTestClient; // For reactive REST API testing

    @Autowired
    private org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate; // For sending sensor data messages

    @BeforeEach
    public void cleanDatabase() {
        // Ensure a clean state by deleting all header records.
        List<HaugfjellMP1Header> headers = mp1HeaderRepository.findAll().collectList().block();
        if (headers != null) {
            headers.forEach(header -> mp1HeaderRepository.delete(header).block());
        }
    }

    /**
     * Test the complete sensor data flow:
     * 1. Send a valid sensor data payload to Kafka.
     * 2. Wait until the header record is persisted.
     * 3. Call the dashboard endpoint and verify aggregated metrics.
     * 4. Call the predictive maintenance endpoint and verify risk calculation.
     * 5. Call the realtime alert endpoint and verify response.
     */
    @Test
    public void testSensorDataFlow() throws Exception {
        // 1. Create and send a sample sensor data payload for station "MP1".
        SensorDataPayload payload = new SensorDataPayload();
        AnalysisHeaderDTO headerDTO = new AnalysisHeaderDTO();
        headerDTO.setMplace("MP1");
        headerDTO.setMstation("Haugfjell");
        payload.setHeader(headerDTO);

        AnalysisMeasurementDTO measurementDTO = new AnalysisMeasurementDTO();
        measurementDTO.setSpdTp1(90.0);
        payload.setMeasurement(measurementDTO);

        String message = objectMapper.writeValueAsString(payload);
        kafkaTemplate.send("sensor-data-topic", message);
        kafkaTemplate.flush();

        // 2. Wait until the header record is persisted.
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    List<HaugfjellMP1Header> headers = mp1HeaderRepository.findAll().collectList().block();
                    assertThat(headers).isNotNull();
                    Optional<HaugfjellMP1Header> headerOpt = headers.stream()
                            .filter(h -> "MP1".equalsIgnoreCase(h.getMplace()))
                            .findFirst();
                    assertThat(headerOpt).isPresent();
                });

        // Retrieve the persisted header to obtain its train_no (analysisId).
        List<HaugfjellMP1Header> headers = mp1HeaderRepository.findAll().collectList().block();
        HaugfjellMP1Header header = headers.stream()
                .filter(h -> "MP1".equalsIgnoreCase(h.getMplace()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Header not found"));
        Integer analysisId = header.getTrainNo();
        assertThat(analysisId).isNotNull();

        // 3. Call the dashboard endpoint to get the latest aggregated sensor metrics.
        webTestClient.get().uri("/api/v1/dashboard/latest/{analysisId}", analysisId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.analysisId").isEqualTo(analysisId)
                .jsonPath("$.averageSpeed").isEqualTo(90.0);

        // 4. Call the predictive maintenance endpoint to verify risk calculation.
        webTestClient.get().uri(uriBuilder -> uriBuilder
                        .path("/api/v1/predictive/{analysisId}")
                        .queryParam("alertEmail", "alerts@example.com")
                        .build(analysisId))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.analysisId").isEqualTo(analysisId)
                .jsonPath("$.riskScore").exists()
                .jsonPath("$.predictionMessage").exists();

        // 5. Call the realtime alert endpoint and verify response.
        webTestClient.post().uri(uriBuilder -> uriBuilder
                        .path("/api/v1/realtime/alert/{analysisId}")
                        .queryParam("alertEmail", "alerts@example.com")
                        .build(analysisId))
                .exchange()
                .expectStatus().isOk();
    }

    /**
     * Test sending multiple sensor data messages.
     * This test sends two messages with different speed values, verifies that at least two headers are persisted,
     * and then checks that each dashboard call returns the correct metrics for its analysisId.
     */
    @Test
    public void testMultipleSensorDataFlow() throws Exception {
        // Send first payload with spdTp1 = 80.0.
        SensorDataPayload payload1 = new SensorDataPayload();
        AnalysisHeaderDTO headerDTO1 = new AnalysisHeaderDTO();
        headerDTO1.setMplace("MP1");
        headerDTO1.setMstation("Haugfjell");
        payload1.setHeader(headerDTO1);
        AnalysisMeasurementDTO measurementDTO1 = new AnalysisMeasurementDTO();
        measurementDTO1.setSpdTp1(80.0);
        payload1.setMeasurement(measurementDTO1);
        kafkaTemplate.send("sensor-data-topic", objectMapper.writeValueAsString(payload1));

        // Send second payload with spdTp1 = 100.0.
        SensorDataPayload payload2 = new SensorDataPayload();
        AnalysisHeaderDTO headerDTO2 = new AnalysisHeaderDTO();
        headerDTO2.setMplace("MP1");
        headerDTO2.setMstation("Haugfjell");
        payload2.setHeader(headerDTO2);
        AnalysisMeasurementDTO measurementDTO2 = new AnalysisMeasurementDTO();
        measurementDTO2.setSpdTp1(100.0);
        payload2.setMeasurement(measurementDTO2);
        kafkaTemplate.send("sensor-data-topic", objectMapper.writeValueAsString(payload2));
        kafkaTemplate.flush();

        // Wait until at least 2 header records are persisted.
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    List<HaugfjellMP1Header> headersList = mp1HeaderRepository.findAll().collectList().block();
                    assertThat(headersList).isNotNull();
                    assertThat(headersList.size()).isGreaterThanOrEqualTo(2);
                });

        // For each header record, call the dashboard endpoint and verify the average speed.
        List<HaugfjellMP1Header> headersList = mp1HeaderRepository.findAll().collectList().block();
        for (HaugfjellMP1Header hdr : headersList) {
            Integer id = hdr.getTrainNo();
            webTestClient.get()
                    .uri("/api/v1/dashboard/latest/{analysisId}", id)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.analysisId").isEqualTo(id)
                    .jsonPath("$.averageSpeed").value(speed -> {
                        Double s = Double.valueOf(speed.toString());
                        assertThat(s).isIn(80.0, 100.0);
                    });
        }
    }

    /**
     * Test sending a malformed sensor data message.
     * This test sends an invalid JSON message to Kafka and asserts that no new header record is created.
     */
    @Test
    public void testMalformedSensorDataMessage() {
        Long initialCount = Optional.ofNullable(mp1HeaderRepository.count().block()).orElse(0L);
        String badMessage = "this-is-not-a-valid-json";
        kafkaTemplate.send("sensor-data-topic", badMessage);
        kafkaTemplate.flush();

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    Long currentCount = Optional.ofNullable(mp1HeaderRepository.count().block()).orElse(0L);
                    assertThat(currentCount).isEqualTo(initialCount);
                });
    }

    /**
     * Test triggering the realtime alert endpoint.
     * This test sends a valid sensor data payload to Kafka, waits for a header record to be persisted,
     * and then calls the realtime alert endpoint.
     */
    @Test
    public void testRealtimeAlertTrigger() throws Exception {
        SensorDataPayload payload = new SensorDataPayload();
        AnalysisHeaderDTO headerDTO = new AnalysisHeaderDTO();
        headerDTO.setMplace("MP1");
        headerDTO.setMstation("Haugfjell");
        payload.setHeader(headerDTO);
        AnalysisMeasurementDTO measurementDTO = new AnalysisMeasurementDTO();
        measurementDTO.setSpdTp1(120.0);
        payload.setMeasurement(measurementDTO);
        String message = objectMapper.writeValueAsString(payload);
        kafkaTemplate.send("sensor-data-topic", message);
        kafkaTemplate.flush();

        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    List<HaugfjellMP1Header> headersList = mp1HeaderRepository.findAll().collectList().block();
                    assertThat(headersList).isNotNull();
                    Optional<HaugfjellMP1Header> headerOpt = headersList.stream()
                            .filter(h -> "MP1".equalsIgnoreCase(h.getMplace()))
                            .findFirst();
                    assertThat(headerOpt).isPresent();
                });

        List<HaugfjellMP1Header> headersList = mp1HeaderRepository.findAll().collectList().block();
        HaugfjellMP1Header header = headersList.stream()
                .filter(h -> "MP1".equalsIgnoreCase(h.getMplace()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Header not found"));
        Integer analysisId = header.getTrainNo();
        assertThat(analysisId).isNotNull();

        webTestClient.post().uri(uriBuilder -> uriBuilder
                        .path("/api/v1/realtime/alert/{analysisId}")
                        .queryParam("alertEmail", "testalerts@example.com")
                        .build(analysisId))
                .exchange()
                .expectStatus().isOk();
    }
}
