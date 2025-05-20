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
        // ... your existing stub logic ...
        Double avg = 85.0, thresh = 80.0;
        if (avg > thresh) {
            String subj = "High Average Speed Alert";
            String txt = "Average speed " + avg + " km/h exceeds threshold!";
            return sendEmailAndPersist(subj, txt);
        }
        return Mono.empty();
    }

    private Mono<Void> sendEmailAndPersist(String subject, String text) {
        return Mono.fromRunnable(() -> {
                    SimpleMailMessage m = new SimpleMailMessage();
                    m.setTo("alert@example.com");
                    m.setSubject(subject);
                    m.setText(text);
                    mailSender.send(m);
                })
                .then(Mono.defer(() -> {
                    AlertHistory e = AlertHistory.builder()
                            .subject(subject)
                            .text(text)
                            .timestamp(LocalDateTime.now())
                            .acknowledged(false)
                            .build();
                    return alertRepo.save(e);
                }))
                .doOnNext(saved -> log.debug("Persisted alert {}", saved.getId()))
                .then();
    }

    @Override
    public Flux<AlertResponse> getAlertHistory() {
        // original, unfiltered history
        return alertRepo.findAllByOrderByTimestampDesc()
                .map(mapper::toDto)
                .map(dto -> {
                    AlertResponse r = new AlertResponse();
                    r.setId(dto.getId());
                    r.setSubject(dto.getSubject());
                    r.setMessage(dto.getText());
                    r.setTimestamp(dto.getTimestamp());
                    r.setAcknowledged(dto.getAcknowledged());
                    r.setAcknowledgedBy(dto.getAcknowledgedBy());
                    r.setTrainNo(dto.getTrainNo());
                    r.setSeverity(dto.getSeverity());
                    return r;
                });
    }

    @Override
    public Flux<AlertResponse> getAlertHistory(
            Integer trainNo,
            LocalDateTime from,
            LocalDateTime to
    ) {
        // filtered history — reuses the no-arg method
        return getAlertHistory()
                .filter(r -> from == null || !r.getTimestamp().isBefore(from))
                .filter(r -> to   == null || !r.getTimestamp().isAfter(to));
        // TODO: once you persist trainNo in AlertHistory, add:
        // .filter(r -> trainNo == null || trainNo.equals(r.getTrainNo()))
    }

    @Override
    public Mono<Void> acknowledgeAlert(AlertAcknowledgeRequest request) {
        return alertRepo.findById(request.getAlertId())
                .flatMap(alert -> {
                    alert.setAcknowledged(true);
                    return alertRepo.save(alert);
                })
                .then()
                .doOnSuccess(v -> log.info("Alert {} acknowledged", request.getAlertId()));
    }
}
