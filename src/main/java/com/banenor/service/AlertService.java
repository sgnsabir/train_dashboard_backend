package com.banenor.service;

import com.banenor.dto.AlertResponse;
import com.banenor.dto.AlertAcknowledgeRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AlertService {
    Mono<Void> checkSensorThresholds();
    Flux<AlertResponse> getAlertHistory();
    Mono<Void> acknowledgeAlert(AlertAcknowledgeRequest request);
}
