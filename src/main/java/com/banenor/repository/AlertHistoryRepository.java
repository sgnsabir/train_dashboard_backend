package com.banenor.repository;

import com.banenor.model.AlertHistory;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface AlertHistoryRepository
        extends R2dbcRepository<AlertHistory, Long>, AlertHistoryRepositoryCustom {

    /** Simple lookup by acknowledged flag (for quick UI toggles). */
    Flux<AlertHistory> findByAcknowledged(boolean acknowledged);
    Flux<AlertHistory> findAllByOrderByTimestampDesc();
}
