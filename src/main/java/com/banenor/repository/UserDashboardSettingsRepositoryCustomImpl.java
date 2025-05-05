package com.banenor.repository;

import com.banenor.model.UserDashboardSettings;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDashboardSettingsRepositoryCustomImpl implements UserDashboardSettingsRepositoryCustom {

    private final DatabaseClient db;

    @Override
    public Flux<UserDashboardSettings> findByUserId(Long userId) {
        var sql = """
            SELECT settings_id,
                   user_id,
                   dashboard_type,
                   settings,
                   created_at,
                   updated_at
              FROM user_dashboard_settings
             WHERE user_id = :userId
            """;
        log.debug("findByUserId SQL: {}", sql);
        return db.sql(sql)
                .bind("userId", userId)
                .map(this::mapRow)
                .all();
    }

    @Override
    public Mono<Void> upsertSetting(Long userId, String dashboardType, String settings) {
        var now = LocalDateTime.now();
        var sql = """
            INSERT INTO user_dashboard_settings
                (user_id, dashboard_type, settings, created_at, updated_at)
            VALUES (:userId, :type, :settings, :now, :now)
            ON CONFLICT (user_id, dashboard_type) DO
              UPDATE SET
                settings   = EXCLUDED.settings,
                updated_at = EXCLUDED.updated_at
            """;
        log.debug("upsertSetting SQL: {} — params: userId={}, dashboardType={}, settings={}",
                sql, userId, dashboardType, settings);
        return db.sql(sql)
                .bind("userId", userId)
                .bind("type", dashboardType)
                .bind("settings", settings)
                .bind("now", now)
                .then();
    }

    @Override
    public Mono<Void> deleteSetting(Long userId, String dashboardType) {
        var sql = """
            DELETE FROM user_dashboard_settings
             WHERE user_id       = :userId
               AND dashboard_type = :type
            """;
        log.debug("deleteSetting SQL: {} — params: userId={}, dashboardType={}", sql, userId, dashboardType);
        return db.sql(sql)
                .bind("userId", userId)
                .bind("type", dashboardType)
                .then();
    }

    private UserDashboardSettings mapRow(Row row, RowMetadata meta) {
        var s = new UserDashboardSettings();
        s.setSettingsId(row.get("settings_id", Long.class));
        s.setUserId(row.get("user_id", Long.class));
        s.setDashboardType(row.get("dashboard_type", String.class));
        s.setSettings(row.get("settings", String.class));
        s.setCreatedAt(row.get("created_at", LocalDateTime.class));
        s.setUpdatedAt(row.get("updated_at", LocalDateTime.class));
        return s;
    }
}
