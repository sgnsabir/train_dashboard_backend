package com.banenor.integration;

import com.banenor.config.MaintenanceProperties;
import com.banenor.model.HaugfjellMP1Axles;
import com.banenor.model.HaugfjellMP1Header;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.service.RealtimeAlertService;
import com.banenor.alert.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@Import(AlertIntegrationTest.TestConfig.class)
public class AlertIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        // Define a bean for NotificationService as a mock.
        @Bean
        public NotificationService notificationService() {
            return mock(NotificationService.class);
        }
    }

    @Autowired
    private HaugfjellMP1HeaderRepository headerRepository;

    @Autowired
    private HaugfjellMP1AxlesRepository axlesRepository;

    @Autowired
    private RealtimeAlertService realtimeAlertService;

    @Autowired
    private MaintenanceProperties maintenanceProperties;

    @Autowired
    private NotificationService notificationService; // Provided by our TestConfig

    private HaugfjellMP1Header testHeader;

    @BeforeEach
    void setUp() {
        // Clean repositories before each test to ensure isolation.
        axlesRepository.deleteAll().block();
        headerRepository.deleteAll().block();

        // Create and persist a header record for station MP1.
        testHeader = HaugfjellMP1Header.builder()
                .mplace("MP1")
                .mstation("Haugfjell")
                .cooLat(60.0)
                .cooLong(10.0)
                .mstartTime(Timestamp.valueOf(LocalDateTime.now().minusMinutes(5)))
                .mstopTime(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        testHeader = headerRepository.save(testHeader).block();
    }

    @Test
    void testRealtimeAlertTriggering_WhenValuesExceedThresholds() {
        // Insert an axle record with sensor values above realtime thresholds.
        HaugfjellMP1Axles axle = HaugfjellMP1Axles.builder()
                .header(testHeader)
                .spdTp1(maintenanceProperties.getRealtimeSpeedThreshold() + 10) // Exceeds speed threshold
                .vviblTp1(maintenanceProperties.getRealtimeVibrationLeftThreshold() + 1)
                .vvibrTp1(maintenanceProperties.getRealtimeVibrationRightThreshold() + 1)
                .vfrcrTp1(maintenanceProperties.getRealtimeVerticalForceRightThreshold() + 50)
                .vfrclTp1(maintenanceProperties.getRealtimeVerticalForceLeftThreshold() + 50)
                .lfrcrTp1(maintenanceProperties.getRealtimeLateralForceRightThreshold() + 50)
                .lvibrTp1(maintenanceProperties.getRealtimeLateralVibrationRightThreshold() + 0.5)
                .build();
        axlesRepository.save(axle).block();

        // Invoke the realtime alert service.
        realtimeAlertService.monitorAndAlert(testHeader.getTrainNo(), "testalerts@example.com").block();

        // Verify that NotificationService.sendAlert() is called at least once for each expected alert type.
        verify(notificationService, atLeastOnce())
                .sendAlert(eq("testalerts@example.com"), contains("Speed Exceeded"), anyString());
        verify(notificationService, atLeastOnce())
                .sendAlert(eq("testalerts@example.com"), contains("Left Vibration Exceeded"), anyString());
        verify(notificationService, atLeastOnce())
                .sendAlert(eq("testalerts@example.com"), contains("Right Vibration Exceeded"), anyString());
        verify(notificationService, atLeastOnce())
                .sendAlert(eq("testalerts@example.com"), contains("Vertical Force Right Exceeded"), anyString());
        verify(notificationService, atLeastOnce())
                .sendAlert(eq("testalerts@example.com"), contains("Vertical Force Left Exceeded"), anyString());
        verify(notificationService, atLeastOnce())
                .sendAlert(eq("testalerts@example.com"), contains("Lateral Force Right Exceeded"), anyString());
        verify(notificationService, atLeastOnce())
                .sendAlert(eq("testalerts@example.com"), contains("Lateral Vibration Right Exceeded"), anyString());
    }

    @Test
    void testRealtimeAlertNotTriggered_WhenValuesBelowThresholds() {
        // Insert an axle record with sensor values below realtime thresholds.
        HaugfjellMP1Axles axle = HaugfjellMP1Axles.builder()
                .header(testHeader)
                .spdTp1(maintenanceProperties.getRealtimeSpeedThreshold() - 5) // Below speed threshold
                .vviblTp1(maintenanceProperties.getRealtimeVibrationLeftThreshold() - 0.5)
                .vvibrTp1(maintenanceProperties.getRealtimeVibrationRightThreshold() - 0.5)
                .vfrcrTp1(maintenanceProperties.getRealtimeVerticalForceRightThreshold() - 10)
                .vfrclTp1(maintenanceProperties.getRealtimeVerticalForceLeftThreshold() - 10)
                .lfrcrTp1(maintenanceProperties.getRealtimeLateralForceRightThreshold() - 10)
                .lvibrTp1(maintenanceProperties.getRealtimeLateralVibrationRightThreshold() - 0.2)
                .build();
        axlesRepository.save(axle).block();

        // Clear any previous interactions with the mock.
        reset(notificationService);

        // Invoke the realtime alert service; since values are below thresholds, no alert should be triggered.
        realtimeAlertService.monitorAndAlert(testHeader.getTrainNo(), "testalerts@example.com").block();

        // Verify that NotificationService.sendAlert() is never called.
        verify(notificationService, never()).sendAlert(anyString(), anyString(), anyString());
    }
}
