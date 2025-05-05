// src/main/java/com/banenor/service/TrainHealthServiceImpl.java
package com.banenor.service;

import com.banenor.dto.TrainHealthDTO;
import com.banenor.mapper.TrainHealthMapper;
import com.banenor.repository.TrainHealthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainHealthServiceImpl implements TrainHealthService {

    private final TrainHealthRepository repository;
    private final TrainHealthMapper mapper;

    @Override
    public Flux<TrainHealthDTO> getHealthForTrain(Integer trainNo) {
        log.debug("Fetching all health records for train {}", trainNo);
        return repository
                .findAllByTrainNoOrderByTimestampDesc(trainNo)
                .map(mapper::toDto)
                .doOnError(e -> log.error("Error retrieving health for train {}", trainNo, e));
    }

    @Override
    public Mono<TrainHealthDTO> getLatestHealth(Integer trainNo) {
        log.debug("Fetching latest health record for train {}", trainNo);
        return repository
                .findTopByTrainNoOrderByTimestampDesc(trainNo)
                .map(mapper::toDto)
                .doOnError(e -> log.error("Error retrieving latest health for train {}", trainNo, e));
    }

    @Override
    public Flux<TrainHealthDTO> findByFilters(Map<String, Object> filters) {
        log.debug("Fetching health records with dynamic filters {}", filters);
        return repository
                .findByDynamicFilters(filters)
                .map(mapper::toDto)
                .doOnError(e -> log.error("Error querying health with filters {}", filters, e));
    }
}
