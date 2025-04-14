package com.banenor.service;

import com.banenor.alert.NotificationService;
import com.banenor.config.MaintenanceProperties;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.util.RepositoryResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@TestPropertySource(locations = "classpath:test.properties")
class RealtimeAlertServiceTest {

    private static final String ALERT_EMAIL = "selbdtest@gmail.com";

    private RepositoryResolver repositoryResolver;
    private NotificationService notificationService;
    private MaintenanceProperties maintenanceProperties;
    private HaugfjellMP1AxlesRepository repository;
    private RealtimeAlertService realtimeAlertService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repositoryResolver = mock(RepositoryResolver.class);
        notificationService = mock(NotificationService.class);
        maintenanceProperties = new MaintenanceProperties();
        // Set thresholds for realtime alerts
        maintenanceProperties.setRealtimeSpeedThreshold(100.0);
        maintenanceProperties.setRealtimeVibrationLeftThreshold(5.0);
        maintenanceProperties.setRealtimeVibrationRightThreshold(5.0);
        maintenanceProperties.setRealtimeVerticalForceRightThreshold(550.0);
        maintenanceProperties.setRealtimeVerticalForceLeftThreshold(550.0);
        maintenanceProperties.setRealtimeLateralForceRightThreshold(350.0);
        maintenanceProperties.setRealtimeLateralVibrationRightThreshold(2.5);

        // Use the concrete repository instead of the abstract one.
        repository = mock(HaugfjellMP1AxlesRepository.class);
        when(repositoryResolver.resolveRepository(anyInt())).thenReturn(Mono.just(repository));

        // Stub all alert calls to return an empty Mono.
        when(notificationService.sendAlert(eq(ALERT_EMAIL), contains("Speed"), anyString()))
                .thenReturn(Mono.empty());
        when(notificationService.sendAlert(eq(ALERT_EMAIL), contains("Left Vibration"), anyString()))
                .thenReturn(Mono.empty());
        when(notificationService.sendAlert(eq(ALERT_EMAIL), contains("Right Vibration"), anyString()))
                .thenReturn(Mono.empty());
        when(notificationService.sendAlert(eq(ALERT_EMAIL), contains("Vertical Force Right"), anyString()))
                .thenReturn(Mono.empty());
        when(notificationService.sendAlert(eq(ALERT_EMAIL), contains("Vertical Force Left"), anyString()))
                .thenReturn(Mono.empty());
        when(notificationService.sendAlert(eq(ALERT_EMAIL), contains("Lateral Force Right"), anyString()))
                .thenReturn(Mono.empty());
        when(notificationService.sendAlert(eq(ALERT_EMAIL), contains("Lateral Vibration Right"), anyString()))
                .thenReturn(Mono.empty());

        realtimeAlertService = new RealtimeAlertServiceImpl(repositoryResolver, notificationService, maintenanceProperties);
    }

    @Test
    void testMonitorAndAlert_TriggersAlerts_WhenThresholdsExceeded() {
        int trainNo = 1;
        // Configure repository mock to return values exceeding thresholds.
        when(repository.findFirstSpdTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(110.0));
        when(repository.findFirstVviblTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(6.0));
        when(repository.findFirstVvibrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(6.0));
        when(repository.findFirstVfrcrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(600.0));
        when(repository.findFirstVfrclTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(600.0));
        when(repository.findFirstLfrcrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(360.0));
        when(repository.findFirstLvibrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(3.0));

        StepVerifier.create(realtimeAlertService.monitorAndAlert(trainNo, ALERT_EMAIL))
                .verifyComplete();

        // Verify that alerts are triggered for each sensor metric exceeding its threshold.
        verify(notificationService, atLeastOnce())
                .sendAlert(eq(ALERT_EMAIL), contains("Speed"), anyString());
        verify(notificationService, atLeastOnce())
                .sendAlert(eq(ALERT_EMAIL), contains("Left Vibration"), anyString());
        verify(notificationService, atLeastOnce())
                .sendAlert(eq(ALERT_EMAIL), contains("Right Vibration"), anyString());
        verify(notificationService, atLeastOnce())
                .sendAlert(eq(ALERT_EMAIL), contains("Vertical Force Right"), anyString());
        verify(notificationService, atLeastOnce())
                .sendAlert(eq(ALERT_EMAIL), contains("Vertical Force Left"), anyString());
        verify(notificationService, atLeastOnce())
                .sendAlert(eq(ALERT_EMAIL), contains("Lateral Force Right"), anyString());
        verify(notificationService, atLeastOnce())
                .sendAlert(eq(ALERT_EMAIL), contains("Lateral Vibration Right"), anyString());
    }

    @Test
    void testMonitorAndAlert_NoAlerts_WhenValuesBelowThreshold() {
        int trainNo = 2;
        when(repository.findFirstSpdTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(90.0));
        when(repository.findFirstVviblTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(4.0));
        when(repository.findFirstVvibrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(4.0));
        when(repository.findFirstVfrcrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(500.0));
        when(repository.findFirstVfrclTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(500.0));
        when(repository.findFirstLfrcrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(300.0));
        when(repository.findFirstLvibrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(2.0));

        StepVerifier.create(realtimeAlertService.monitorAndAlert(trainNo, ALERT_EMAIL))
                .verifyComplete();

        verify(notificationService, never()).sendAlert(anyString(), anyString(), anyString());
    }

    @Test
    void testMonitorAndAlert_NoAlerts_WhenValuesAtThreshold() {
        int trainNo = 3;
        when(repository.findFirstSpdTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(100.0));
        when(repository.findFirstVviblTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(5.0));
        when(repository.findFirstVvibrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(5.0));
        when(repository.findFirstVfrcrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(550.0));
        when(repository.findFirstVfrclTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(550.0));
        when(repository.findFirstLfrcrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(350.0));
        when(repository.findFirstLvibrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(2.5));

        StepVerifier.create(realtimeAlertService.monitorAndAlert(trainNo, ALERT_EMAIL))
                .verifyComplete();

        verify(notificationService, never()).sendAlert(anyString(), anyString(), anyString());
    }

    @Test
    void testMonitorAndAlert_NoAlerts_WhenSomeValuesAreNull() {
        int trainNo = 4;
        when(repository.findFirstSpdTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.empty());
        when(repository.findFirstVviblTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(4.0));
        when(repository.findFirstVvibrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.empty());
        when(repository.findFirstVfrcrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(500.0));
        when(repository.findFirstVfrclTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.empty());
        when(repository.findFirstLfrcrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(300.0));
        when(repository.findFirstLvibrTp1ByTrainNoOrderByCreatedAtDesc(trainNo)).thenReturn(Mono.just(2.0));

        StepVerifier.create(realtimeAlertService.monitorAndAlert(trainNo, ALERT_EMAIL))
                .verifyComplete();

        verify(notificationService, never()).sendAlert(anyString(), anyString(), anyString());
    }
}
