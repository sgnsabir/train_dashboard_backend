package com.banenor.service;

import com.banenor.dto.RawDataResponse;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

public interface StationAxlesService {
    Flux<RawDataResponse> getHistoricalData(
            Integer trainNo, String station, LocalDateTime start, LocalDateTime end
    );

    Flux<RawDataResponse> streamRawData(
            Integer trainNo, String station, LocalDateTime start, LocalDateTime end
    );

    Flux<RawDataResponse> getRawAxlesData(Integer trainNo,
                                          LocalDateTime start,
                                          LocalDateTime end);
}
