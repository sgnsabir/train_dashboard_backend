package com.banenor.repository;

import com.banenor.model.DigitalTwin;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class DigitalTwinRepositoryCustomImpl implements DigitalTwinRepositoryCustom {

    private final DatabaseClient client;

    // now includes all columns needed by DigitalTwinInsightDTO
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "asset_id", "recorded_at", "metric_type", "metric_value",
            "component_name", "location", "status", "risk_score"
    );

    @Override
    public Flux<DigitalTwin> findByFilters(Map<String, Object> filters, int page, int size) {
        var sql = new StringBuilder("""
            SELECT asset_id, recorded_at, metric_type, metric_value,
                   component_name, location, status, risk_score
              FROM digital_twins
             WHERE 1=1
            """);
        var params = new LinkedHashMap<String, Object>();
        int idx = 0;
        for (var e : filters.entrySet()) {
            if (!ALLOWED_COLUMNS.contains(e.getKey())) continue;
            String pname = "p" + idx++;
            sql.append(" AND ").append(e.getKey()).append(" = :").append(pname);
            params.put(pname, e.getValue());
        }
        sql.append(" ORDER BY recorded_at DESC")
                .append(" LIMIT ").append(size)
                .append(" OFFSET ").append((long) page * size);

        var spec = client.sql(sql.toString());
        for (var p : params.entrySet()) spec = spec.bind(p.getKey(), p.getValue());
        return spec.map(this::mapRow).all();
    }

    @Override
    public Mono<Long> countByFilters(Map<String, Object> filters) {
        var sql = new StringBuilder("""
            SELECT COUNT(*) AS cnt
              FROM digital_twins
             WHERE 1=1
            """);
        var params = new LinkedHashMap<String, Object>();
        int idx = 0;
        for (var e : filters.entrySet()) {
            if (!ALLOWED_COLUMNS.contains(e.getKey())) continue;
            String pname = "p" + idx++;
            sql.append(" AND ").append(e.getKey()).append(" = :").append(pname);
            params.put(pname, e.getValue());
        }
        var spec = client.sql(sql.toString());
        for (var p : params.entrySet()) spec = spec.bind(p.getKey(), p.getValue());
        return spec.map((row, md) -> row.get("cnt", Long.class)).one();
    }

    private DigitalTwin mapRow(Row row, RowMetadata rm) {
        return DigitalTwin.builder()
                .assetId(row.get("asset_id", Integer.class))
                .recordedAt(row.get("recorded_at", java.time.LocalDateTime.class))
                .metricType(row.get("metric_type", String.class))
                .metricValue(row.get("metric_value", Double.class))
                .componentName(row.get("component_name", String.class))
                .location(row.get("location", String.class))
                .status(row.get("status", String.class))
                .riskScore(row.get("risk_score", Double.class))
                .build();
    }
}
