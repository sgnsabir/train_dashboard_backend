// src/main/java/com/banenor/service/PerformanceServiceImpl.java
package com.banenor.service;

import com.banenor.dto.PerformanceDTO;
import com.banenor.model.AbstractAxles;
import com.banenor.util.RepositoryResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.beans.PropertyDescriptor;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TreeMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl implements PerformanceService {

    private final RepositoryResolver repositoryResolver;

    // matches fields like spdTp1, spdTp2, spdTp3, etc.
    private static final Pattern SPEED_TP_FIELD = Pattern.compile("^spdTp(\\d+)$");

    @Override
    public Flux<PerformanceDTO> getPerformanceData(LocalDateTime start, LocalDateTime end) {
        LocalDateTime effectiveEnd = (end == null) ? LocalDateTime.now(ZoneOffset.UTC) : end;
        LocalDateTime effectiveStart = (start == null) ? effectiveEnd.minusDays(7) : start;

        log.info("Fetching performance data from {} to {}", effectiveStart, effectiveEnd);

        return repositoryResolver.resolveRepository(null)    // pass null to get *all* axles repos
                .flatMapMany(repo -> repo.findAll().cast(AbstractAxles.class))
                .filter(a -> inRange(a.getCreatedAt(), effectiveStart, effectiveEnd))
                .flatMap(this::expandByTp)
                .doOnError(e -> log.error("Error fetching performance data", e))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private boolean inRange(LocalDateTime ts, LocalDateTime start, LocalDateTime end) {
        return !ts.isBefore(start) && !ts.isAfter(end);
    }

    private Flux<PerformanceDTO> expandByTp(AbstractAxles axle) {
        BeanWrapper bw = new BeanWrapperImpl(axle);
        Map<Integer, PerformanceDTO> map = new TreeMap<>();

        for (PropertyDescriptor pd : bw.getPropertyDescriptors()) {
            Matcher m = SPEED_TP_FIELD.matcher(pd.getName());
            if (!m.matches()) continue;

            int idx = Integer.parseInt(m.group(1));
            Object raw = bw.getPropertyValue(pd.getName());
            Double speed = (raw instanceof Number) ? ((Number) raw).doubleValue() : null;

            PerformanceDTO dto = map.computeIfAbsent(idx, i -> {
                PerformanceDTO d = new PerformanceDTO();
                d.setCreatedAt(axle.getCreatedAt());
                d.setMeasurementPoint("TP" + i);
                return d;
            });
            dto.setSpeed(speed);
            // acceleration left null — compute in future if needed
        }

        return Flux.fromIterable(map.values());
    }
}
