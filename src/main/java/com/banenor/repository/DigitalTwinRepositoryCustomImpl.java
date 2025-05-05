package com.banenor.repository;

import com.banenor.model.DigitalTwin;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of our custom DigitalTwinRepositoryCustom.
 * Builds parameterized SQL with a whitelist of columns and
 * falls back to DatabaseClient for dynamic queries & pagination.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DigitalTwinRepositoryCustomImpl implements DigitalTwinRepositoryCustom {

    private final DatabaseClient client;

    /**
     * Only these columns are allowed to appear in filters
     * (prevents SQL injection via untrusted column names).
     */
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "asset_id", "status", "sensor_summary", "updated_at"
    );

    @Override
    public Flux<DigitalTwin> findByFilters(Map<String, Object> filters, int page, int size) {
        var sql = new StringBuilder("""
            SELECT asset_id, status, sensor_summary, updated_at
              FROM digital_twin
             WHERE 1 = 1
            """);
        var params = new LinkedHashMap<String, Object>();
        int idx = 0;

        for (var e : filters.entrySet()) {
            var col = e.getKey();
            if (!ALLOWED_COLUMNS.contains(col)) {
                log.warn("Ignoring unsupported filter column: {}", col);
                continue;
            }
            var param = "p" + idx++;
            sql.append(" AND ").append(col).append(" = :").append(param);
            params.put(param, e.getValue());
        }

        sql.append(" ORDER BY updated_at DESC")
                .append(" LIMIT ").append(size)
                .append(" OFFSET ").append((long) page * size);

        var spec = client.sql(sql.toString());
        for (var p : params.entrySet()) {
            spec = spec.bind(p.getKey(), p.getValue());
        }

        return spec.map(this::mapRow).all();
    }

    @Override
    public Mono<Long> countByFilters(Map<String, Object> filters) {
        var sql = new StringBuilder("SELECT COUNT(*) AS cnt FROM digital_twin WHERE 1 = 1");
        var params = new LinkedHashMap<String, Object>();
        int idx = 0;

        for (var e : filters.entrySet()) {
            var col = e.getKey();
            if (!ALLOWED_COLUMNS.contains(col)) {
                log.warn("Ignoring unsupported filter column: {}", col);
                continue;
            }
            var param = "p" + idx++;
            sql.append(" AND ").append(col).append(" = :").append(param);
            params.put(param, e.getValue());
        }

        var spec = client.sql(sql.toString());
        for (var p : params.entrySet()) {
            spec = spec.bind(p.getKey(), p.getValue());
        }

        return spec.map((row, md) -> row.get("cnt", Long.class)).one();
    }

    private DigitalTwin mapRow(Row row, RowMetadata meta) {
        // assumes DigitalTwin has setters or a builder
        return DigitalTwin.builder()
                .assetId(row.get("asset_id", Integer.class))
                .status(row.get("status", String.class))
                .sensorSummary(row.get("sensor_summary", String.class))
                .updatedAt(row.get("updated_at", LocalDateTime.class))
                .build();
    }
}
