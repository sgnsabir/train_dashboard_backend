package com.banenor.service;

import reactor.core.publisher.Mono;

public interface RealtimeAlertService {
    Mono<Void> monitorAndAlert(Integer trainNo, String alertEmail);
}
