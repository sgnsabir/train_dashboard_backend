package com.banenor.service;

import com.banenor.dto.AlertResponse;
import com.banenor.dto.AlertAcknowledgeRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AlertServiceImpl implements AlertService {

    private final JavaMailSender mailSender;
    private final AtomicLong alertIdGenerator = new AtomicLong(1);
    private final List<AlertResponse> alertHistory = new CopyOnWriteArrayList<>();

    public AlertServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Scheduled(fixedRateString = "${alert.check.interval.ms:60000}")
    public void scheduledCheckSensorThresholds() {
        checkSensorThresholds().subscribe();
    }

    @Override
    public Mono<Void> checkSensorThresholds() {
        // Stubbed implementation: in real code, query aggregated sensor data reactively.
        Double averageSpeed = 85.0;
        Double speedThreshold = 80.0;
        if (averageSpeed > speedThreshold) {
            return sendEmailAlert("High Average Speed Alert",
                    "Average speed " + averageSpeed + " km/h exceeds threshold!")
                    .then();
        }
        return Mono.empty();
    }

    private Mono<Void> sendEmailAlert(String subject, String text) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("alert@example.com");
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            AlertResponse alertResponse = new AlertResponse();
            alertResponse.setId(alertIdGenerator.getAndIncrement());
            alertResponse.setSubject(subject);
            alertResponse.setText(text);
            alertResponse.setTimestamp(LocalDateTime.now());
            alertResponse.setAcknowledged(false);
            alertHistory.add(alertResponse);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Flux<AlertResponse> getAlertHistory() {
        return Flux.fromIterable(alertHistory);
    }

    @Override
    public Mono<Void> acknowledgeAlert(AlertAcknowledgeRequest request) {
        return Mono.fromRunnable(() -> {
            alertHistory.stream()
                    .filter(alert -> alert.getId().equals(request.getAlertId()))
                    .findFirst()
                    .ifPresent(alert -> alert.setAcknowledged(true));
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
