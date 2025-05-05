package com.banenor.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class HaugfjellMP3AxlesRepositoryCustomImpl implements HaugfjellAxlesRepositoryCustom {

    private final DatabaseClient db;

    // Whitelist of permitted numeric TP columns
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "spd_tp1","spd_tp2","spd_tp3","spd_tp5","spd_tp6","spd_tp8",
            "aoa_tp1","aoa_tp2","aoa_tp3","aoa_tp5","aoa_tp6","aoa_tp8",
            "vvibl_tp1","vvibl_tp2","vvibl_tp3","vvibl_tp5","vvibl_tp6","vvibl_tp8",
            "vvibr_tp1","vvibr_tp2","vvibr_tp3","vvibr_tp5","vvibr_tp6","vvibr_tp8",
            "vfrcl_tp1","vfrcl_tp2","vfrcl_tp3","vfrcl_tp5","vfrcl_tp6","vfrcl_tp8",
            "vfrcr_tp1","vfrcr_tp2","vfrcr_tp3","vfrcr_tp5","vfrcr_tp6","vfrcr_tp8",
            "lfrcl_tp1","lfrcl_tp2","lfrcl_tp3","lfrcl_tp5","lfrcl_tp6",
            "lfrcr_tp1","lfrcr_tp2","lfrcr_tp3","lfrcr_tp5","lfrcr_tp6",
            "lvibl_tp1","lvibl_tp2","lvibl_tp3","lvibl_tp5","lvibl_tp6",
            "lvibr_tp1","lvibr_tp2","lvibr_tp3","lvibr_tp5","lvibr_tp6"
    );

    public HaugfjellMP3AxlesRepositoryCustomImpl(DatabaseClient db) {
        this.db = db;
    }

    @Override
    public Flux<Map<String, Object>> findDynamicAggregationsByTrain(
            Integer trainNo,
            LocalDateTime start,
            LocalDateTime end,
            String column,
            int offset,
            int limit
    ) {
        // 1) Validate
        if (trainNo == null || start == null || end == null) {
            return Flux.error(new IllegalArgumentException("trainNo, start and end must be non-null"));
        }
        if (!ALLOWED_COLUMNS.contains(column)) {
            return Flux.error(new IllegalArgumentException("Column not allowed: " + column));
        }
        if (offset < 0 || limit <= 0) {
            return Flux.error(new IllegalArgumentException("Invalid pagination parameters"));
        }

        // 2) Build SQL
        String sql = """
            SELECT vit
                 , AVG(%1$s) AS avg
                 , MIN(%1$s) AS min
                 , MAX(%1$s) AS max
              FROM haugfjell_mp1_axles
             WHERE train_no = :trainNo
               AND created_at >= :start
               AND created_at <= :end
             GROUP BY vit
             ORDER BY vit
             LIMIT :limit
             OFFSET :offset
            """.formatted(column);

        log.debug("MP1 dynamic aggregation SQL: {}", sql);

        // 3) Execute reactively
        return db.sql(sql)
                .bind("trainNo", trainNo)
                .bind("start", start)
                .bind("end", end)
                .bind("limit", limit)
                .bind("offset", offset)
                .map((row, meta) -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("vit",      row.get("vit", String.class));
                    result.put("avg",      row.get("avg", Double.class));
                    result.put("min",      row.get("min", Double.class));
                    result.put("max",      row.get("max", Double.class));
                    return result;
                })
                .all()
                ;
    }
}
