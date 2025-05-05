package com.banenor.repository;

import com.banenor.model.AlertHistory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AlertHistoryRepositoryImpl implements AlertHistoryRepositoryCustom {

    private final DatabaseClient client;

    @Override
    public Flux<AlertHistory> findByFilters(Map<String, Object> filters, int page, int size) {
        log.debug("findByFilters called with filters={}, page={}, size={}", filters, page, size);

        StringBuilder sql = new StringBuilder(
                "SELECT id, subject, text, timestamp, acknowledged " +
                        "FROM alert_history"
        );

        StringJoiner where = new StringJoiner(" AND ");
        if (filters.containsKey("acknowledged"))  where.add(" acknowledged = :acknowledged ");
        if (filters.containsKey("from"))          where.add(" timestamp >= :from ");
        if (filters.containsKey("to"))            where.add(" timestamp <= :to ");
        if (filters.containsKey("subjectContains")) where.add(" subject ILIKE '%' || :subjectContains || '%' ");

        if (where.length() > 0) {
            sql.append(" WHERE ").append(where);
        }

        sql.append(" ORDER BY timestamp DESC")
                .append(" LIMIT ").append(size)
                .append(" OFFSET ").append((long) page * size);

        DatabaseClient.GenericExecuteSpec spec = client.sql(sql.toString());

        if (filters.containsKey("acknowledged")) {
            spec = spec.bind("acknowledged", Parameter.fromOrEmpty(filters.get("acknowledged"), Boolean.class));
        }
        if (filters.containsKey("from")) {
            spec = spec.bind("from", Parameter.fromOrEmpty(filters.get("from"), LocalDateTime.class));
        }
        if (filters.containsKey("to")) {
            spec = spec.bind("to", Parameter.fromOrEmpty(filters.get("to"), LocalDateTime.class));
        }
        if (filters.containsKey("subjectContains")) {
            spec = spec.bind("subjectContains", Parameter.fromOrEmpty(filters.get("subjectContains"), String.class));
        }

        return spec.map(this::mapRow).all();
    }

    @Override
    public Mono<Long> countByFilters(Map<String, Object> filters) {
        log.debug("countByFilters called with filters={}", filters);

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS cnt FROM alert_history");
        StringJoiner where = new StringJoiner(" AND ");
        if (filters.containsKey("acknowledged"))  where.add(" acknowledged = :acknowledged ");
        if (filters.containsKey("from"))          where.add(" timestamp >= :from ");
        if (filters.containsKey("to"))            where.add(" timestamp <= :to ");
        if (filters.containsKey("subjectContains")) where.add(" subject ILIKE '%' || :subjectContains || '%' ");

        if (where.length() > 0) {
            sql.append(" WHERE ").append(where);
        }

        DatabaseClient.GenericExecuteSpec spec = client.sql(sql.toString());

        if (filters.containsKey("acknowledged")) {
            spec = spec.bind("acknowledged", Parameter.fromOrEmpty(filters.get("acknowledged"), Boolean.class));
        }
        if (filters.containsKey("from")) {
            spec = spec.bind("from", Parameter.fromOrEmpty(filters.get("from"), LocalDateTime.class));
        }
        if (filters.containsKey("to")) {
            spec = spec.bind("to", Parameter.fromOrEmpty(filters.get("to"), LocalDateTime.class));
        }
        if (filters.containsKey("subjectContains")) {
            spec = spec.bind("subjectContains", Parameter.fromOrEmpty(filters.get("subjectContains"), String.class));
        }

        return spec
                .map((row, meta) -> row.get("cnt", Long.class))
                .one();
    }

    /**
     * Map a database row into our domain object.
     */
    private AlertHistory mapRow(Row row, RowMetadata meta) {
        return AlertHistory.builder()
                .id(row.get("id", Long.class))
                .subject(row.get("subject", String.class))
                .text(row.get("text", String.class))
                .timestamp(row.get("timestamp", LocalDateTime.class))
                .acknowledged(row.get("acknowledged", Boolean.class))
                .build();
    }
}
