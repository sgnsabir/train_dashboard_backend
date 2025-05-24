package com.banenor.service;

import com.banenor.dto.AlertAcknowledgeRequest;
import com.banenor.dto.AlertResponse;
import com.banenor.mapper.AlertHistoryMapper;
import com.banenor.model.AlertHistory;
import com.banenor.repository.AlertHistoryRepository;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final MailClient mailClient;
    private final AlertHistoryRepository alertRepo;
    private final AlertHistoryMapper mapper;

    @Value("${mail.from}")
    private String fromAddress;

    @Value("${alert.email.to}")
    private String alertToAddress;

    @Scheduled(fixedRateString = "${alert.check.interval.ms:60000}")
    public void scheduledCheckSensorThresholds() {
        checkSensorThresholds()
                .doOnError(err -> log.error("Scheduled sensor check failed", err))
                .subscribe();
    }

    @Override
    public Mono<Void> checkSensorThresholds() { //its only demo purpose, will be removed in production and only RealtimeAlertService will work further
        double avg = 85.0, thresh = 80.0;
        if (avg > thresh) {
            String subject = "High Average Speed Alert";
            String body    = "Average speed " + avg + " km/h exceeds threshold!";
            return sendEmailAndPersist(subject, body);
        }
        return Mono.empty();
    }

    private Mono<Void> sendEmailAndPersist(String subject, String body) {
        MailMessage msg = new MailMessage()
                .setFrom(fromAddress)
                .setTo(alertToAddress)
                .setSubject(subject)
                .setText(body);

        return Mono.<Void>create(sink ->
                        mailClient.sendMail(msg, ar -> {
                            if (ar.succeeded()) {
                                log.info("Alert email sent to {} (subject={})", alertToAddress, subject);
                                sink.success();
                            } else {
                                log.error("Failed to send alert email to {}", alertToAddress, ar.cause());
                                sink.error(ar.cause());
                            }
                        })
                )
                .then(Mono.defer(() -> {
                    AlertHistory entry = AlertHistory.builder()
                            .subject(subject)
                            .text(body)
                            .timestamp(LocalDateTime.now())
                            .acknowledged(false)
                            .build();
                    return alertRepo.save(entry);
                }))
                .doOnNext(saved -> log.debug("Persisted alert history id={}", saved.getId()))
                .then();
    }

    @Override
    public Flux<AlertResponse> getAlertHistory() {
        return alertRepo.findAllByOrderByTimestampDesc()
                .map(mapper::toDto)
                .map(r -> {
                    var dto = new AlertResponse();
                    dto.setId(r.getId());
                    dto.setSubject(r.getSubject());
                    dto.setMessage(r.getText());
                    dto.setTimestamp(r.getTimestamp());
                    dto.setAcknowledged(r.getAcknowledged());
                    dto.setAcknowledgedBy(r.getAcknowledgedBy());
                    dto.setTrainNo(r.getTrainNo());
                    dto.setSeverity(r.getSeverity());
                    return dto;
                });
    }

    @Override
    public Flux<AlertResponse> getAlertHistory(Integer trainNo, LocalDateTime from, LocalDateTime to) {
        return getAlertHistory()
                .filter(r -> from == null || !r.getTimestamp().isBefore(from))
                .filter(r -> to   == null || !r.getTimestamp().isAfter(to));
    }

    @Override
    public Mono<Void> acknowledgeAlert(AlertAcknowledgeRequest req) {
        log.info("Acknowledging alert id={} by {}", req.getAlertId(), req.getAcknowledgedBy());
        return alertRepo.findById(req.getAlertId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Alert not found: " + req.getAlertId())))
                .flatMap(alert -> {
                    alert.setAcknowledged(true);
                    alert.setAcknowledgedBy(req.getAcknowledgedBy());
                    return alertRepo.save(alert);
                })
                .doOnSuccess(saved -> log.debug("Alert {} marked acknowledged by {}", req.getAlertId(), req.getAcknowledgedBy()))
                .then();
    }
}
