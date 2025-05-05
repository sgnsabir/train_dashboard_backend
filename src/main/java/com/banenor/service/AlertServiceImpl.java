package com.banenor.service;

import com.banenor.dto.AlertAcknowledgeRequest;
import com.banenor.dto.AlertResponse;
import com.banenor.mapper.AlertHistoryMapper;
import com.banenor.model.AlertHistory;
import com.banenor.repository.AlertHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final JavaMailSender mailSender;
    private final AlertHistoryRepository alertRepo;
    private final AlertHistoryMapper mapper;

    @Scheduled(fixedRateString = "${alert.check.interval.ms:60000}")
    public void scheduledCheckSensorThresholds() {
        checkSensorThresholds().subscribe();
    }

    @Override
    public Mono<Void> checkSensorThresholds() {
        // stubbed sensor‐threshold logic...
        Double averageSpeed = 85.0;
        Double speedThreshold = 80.0;
        if (averageSpeed > speedThreshold) {
            String subject = "High Average Speed Alert";
            String text = "Average speed " + averageSpeed + " km/h exceeds threshold!";
            return sendEmailAndPersist(subject, text);
        }
        return Mono.empty();
    }

    private Mono<Void> sendEmailAndPersist(String subject, String text) {
        return Mono.fromRunnable(() -> {
                    // send email
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo("alert@example.com");
                    message.setSubject(subject);
                    message.setText(text);
                    mailSender.send(message);
                })
                .then(Mono.defer(() -> {
                    // persist
                    AlertHistory entity = AlertHistory.builder()
                            .subject(subject)
                            .text(text)
                            .timestamp(LocalDateTime.now())
                            .acknowledged(false)
                            .build();
                    return alertRepo.save(entity);
                }))
                .doOnNext(saved -> log.debug("Persisted alert {}", saved.getId()))
                .then();
    }

    @Override
    public Flux<AlertResponse> getAlertHistory() {
        return alertRepo.findAllByOrderByTimestampDesc()
                .map(mapper::toDto)
                // convert AlertHistoryDTO → AlertResponse
                .map(dto -> {
                    AlertResponse r = new AlertResponse();
                    r.setId(dto.getId());
                    r.setSubject(dto.getSubject());
                    r.setMessage(dto.getText());
                    r.setTimestamp(dto.getTimestamp());
                    r.setAcknowledged(dto.getAcknowledged());
                    return r;
                });
    }

    @Override
    public Mono<Void> acknowledgeAlert(AlertAcknowledgeRequest request) {
        return alertRepo.findById(request.getAlertId())
                .flatMap(alert -> {
                    alert.setAcknowledged(true);
                    return alertRepo.save(alert);
                })
                .then();
    }
}
