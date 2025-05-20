package com.banenor.service;

import com.banenor.dto.AlertResponse;
import com.banenor.dto.AlertAcknowledgeRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface AlertService {

    Mono<Void> checkSensorThresholds();
    Flux<AlertResponse> getAlertHistory();
    Flux<AlertResponse> getAlertHistory(
            Integer trainNo,
            LocalDateTime from,
            LocalDateTime to
    );
    Mono<Void> acknowledgeAlert(AlertAcknowledgeRequest request);
}
