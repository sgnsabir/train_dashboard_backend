package com.banenor.service;

import reactor.core.publisher.Mono;

public interface SystemHealthService {
    /**
     * Retrieves the current system health status reactively.
     *
     * @return a Mono containing a String representing the system status.
     */
    Mono<String> getSystemStatus();
}
