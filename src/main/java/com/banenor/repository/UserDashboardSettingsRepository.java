package com.banenor.repository;

import com.banenor.model.UserDashboardSettings;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

/**
 * R2DBC repository for per-user dashboard preferences.
 * Extends the custom interface for dynamic / DatabaseClient-driven operations.
 */
public interface UserDashboardSettingsRepository
        extends R2dbcRepository<UserDashboardSettings, Long>,
        UserDashboardSettingsRepositoryCustom {
    // All CRUD + custom upsert/find/delete are exposed reactively.
}
