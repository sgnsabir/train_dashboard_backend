// src/main/java/com/banenor/repository/TrainHealthRepositoryImpl.java
package com.banenor.repository;

import com.banenor.model.TrainHealth;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TrainHealthRepositoryImpl implements TrainHealthRepositoryCustom {

    private final DatabaseClient db;

    @Override
    public Flux<TrainHealth> findByDynamicFilters(Map<String, Object> filters) {
        var baseSql = "SELECT id, train_no, health_score, fault_count, timestamp FROM train_health";
        var sql = new StringBuilder(baseSql);

        if (filters != null && !filters.isEmpty()) {
            sql.append(" WHERE ");
            var sj = new StringJoiner(" AND ");
            filters.keySet().forEach(col -> sj.add(col + " = :" + col));
            sql.append(sj.toString());
        }

        log.debug("Dynamic TrainHealth query: {}", sql);
        var spec = db.sql(sql.toString());
        if (filters != null) {
            for (var e : filters.entrySet()) {
                spec = spec.bind(e.getKey(), e.getValue());
            }
        }

        return spec
                .map(this::toEntity)
                .all()
                .doOnError(e -> log.error("Error executing dynamic TrainHealth query", e));
    }

    private TrainHealth toEntity(Row row, RowMetadata meta) {
        return TrainHealth.builder()
                .id(row.get("id", Integer.class))
                .trainNo(row.get("train_no", Integer.class))
                .healthScore(row.get("health_score", Double.class))
                .faultCount(row.get("fault_count", Integer.class))
                .timestamp(row.get("timestamp", LocalDateTime.class))
                .build();
    }
}
