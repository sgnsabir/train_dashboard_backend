package com.banenor.service;

import com.banenor.dto.PerformanceDTO;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

public interface PerformanceService {
    Flux<PerformanceDTO> getPerformanceData(LocalDateTime start, LocalDateTime end);
}
