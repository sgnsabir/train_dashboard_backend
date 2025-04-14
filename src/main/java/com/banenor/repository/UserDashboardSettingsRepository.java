package com.banenor.repository;

import com.banenor.model.UserDashboardSettings;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface UserDashboardSettingsRepository extends R2dbcRepository<UserDashboardSettings, Long> {
    Mono<UserDashboardSettings> findByUserIdAndDashboardType(Long userId, String dashboardType);
}
