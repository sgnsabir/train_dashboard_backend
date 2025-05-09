package com.banenor.repository;

import com.banenor.model.DigitalTwin;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface DigitalTwinRepository
        extends R2dbcRepository<DigitalTwin, Integer>,
        DigitalTwinRepositoryCustom {
    Mono<DigitalTwin> findTopByAssetIdOrderByRecordedAtDesc(Integer assetId);
}
