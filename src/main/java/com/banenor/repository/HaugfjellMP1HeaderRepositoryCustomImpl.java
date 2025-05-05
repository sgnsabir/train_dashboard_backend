package com.banenor.repository;

import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class HaugfjellMP1HeaderRepositoryCustomImpl implements HaugfjellHeaderRepositoryCustom {

    private final DatabaseClient db;

    /**
     * Only these metric keys are allowed; protects against SQL injection.
     */
    private static final Set<String> ALLOWED_METRICS = Set.of(
            "spd", "aoa", "vvibl", "vvibr",
            "vfrcl", "vfrcr", "lfrcl", "lfrcr",
            "lvibl", "lvibr"
    );

    @Override
    public Flux<Map<String, Object>> findHeaderWithAxleAggregates(
            Integer trainNo,
            LocalDateTime axleStart,
            LocalDateTime axleEnd,
            List<String> metrics,
            int page,
            int size
    ) {
        // 1) sanitize & dedupe requested metrics
        List<String> cols = metrics.stream()
                .filter(ALLOWED_METRICS::contains)
                .distinct()
                .collect(Collectors.toList());

        if (cols.isEmpty()) {
            log.warn("No valid metrics requested, defaulting to ['spd']");
            cols = List.of("spd");
        }

        // 2) build SELECT clause dynamically
        String selectAggs = cols.stream()
                .map(this::buildAvgExpression)
                .collect(Collectors.joining(", "));

        String sql = ""
                + "SELECT h.*, " + selectAggs + " "
                + "FROM haugfjell_mp1_header h "
                + "LEFT JOIN haugfjell_mp1_axles a "
                + "  ON a.train_no = h.train_no "
                + "  AND a.created_at BETWEEN :axleStart AND :axleEnd "
                + "WHERE h.train_no = :trainNo "
                + "GROUP BY h.id "
                + "ORDER BY h.mstart_time DESC "
                + "LIMIT :limit OFFSET :offset";

        log.debug("Executing header+axle aggregate SQL: {}", sql);

        return db.sql(sql)
                .bind("trainNo", trainNo)
                .bind("axleStart", axleStart)
                .bind("axleEnd", axleEnd)
                .bind("limit", size)
                .bind("offset", (long) page * size)
                .map(this::mapRow)
                .all();
    }

    /**
     * Returns the correct AVG(….) snippet for the given metric key.
     */
    private String buildAvgExpression(String metric) {
        switch (metric) {
            case "spd":
                return "AVG((a.spd_tp1 + a.spd_tp2 + a.spd_tp3 + a.spd_tp5 + a.spd_tp6 + a.spd_tp8)/6.0) AS avg_spd";
            case "aoa":
                return "AVG((a.aoa_tp1 + a.aoa_tp2 + a.aoa_tp3 + a.aoa_tp5 + a.aoa_tp6 + a.aoa_tp8)/6.0) AS avg_aoa";
            case "vvibl":
                return "AVG((a.vvibl_tp1 + a.vvibl_tp2 + a.vvibl_tp3 + a.vvibl_tp5 + a.vvibl_tp6 + a.vvibl_tp8)/6.0) AS avg_vvibl";
            case "vvibr":
                return "AVG((a.vvibr_tp1 + a.vvibr_tp2 + a.vvibr_tp3 + a.vvibr_tp5 + a.vvibr_tp6 + a.vvibr_tp8)/6.0) AS avg_vvibr";
            case "vfrcl":
                return "AVG((a.vfrcl_tp1 + a.vfrcl_tp2 + a.vfrcl_tp3 + a.vfrcl_tp5 + a.vfrcl_tp6 + a.vfrcl_tp8)/6.0) AS avg_vfrcl";
            case "vfrcr":
                return "AVG((a.vfrcr_tp1 + a.vfrcr_tp2 + a.vfrcr_tp3 + a.vfrcr_tp5 + a.vfrcr_tp6 + a.vfrcr_tp8)/6.0) AS avg_vfrcr";
            case "lfrcl":
                return "AVG((a.lfrcl_tp1 + a.lfrcl_tp2 + a.lfrcl_tp3 + a.lfrcl_tp5 + a.lfrcl_tp6)/5.0) AS avg_lfrcl";
            case "lfrcr":
                return "AVG((a.lfrcr_tp1 + a.lfrcr_tp2 + a.lfrcr_tp3 + a.lfrcr_tp5 + a.lfrcr_tp6)/5.0) AS avg_lfrcr";
            case "lvibl":
                return "AVG((a.lvibl_tp1 + a.lvibl_tp2 + a.lvibl_tp3 + a.lvibl_tp5 + a.lvibl_tp6)/5.0) AS avg_lvibl";
            case "lvibr":
                return "AVG((a.lvibr_tp1 + a.lvibr_tp2 + a.lvibr_tp3 + a.lvibr_tp5 + a.lvibr_tp6)/5.0) AS avg_lvibr";
            default:
                throw new IllegalArgumentException("Unsupported metric: " + metric);
        }
    }

    /**
     * Maps each row into a name→value map, including both header and aggregated columns.
     */
    private Map<String, Object> mapRow(Row row, RowMetadata md) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (ColumnMetadata cm : md.getColumnMetadatas()) {
            String name = cm.getName();
            m.put(name, row.get(name));
        }
        return m;
    }
}
