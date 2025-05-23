// src/main/java/com/banenor/service/DigitalTwinServiceImpl.java
package com.banenor.service;

import com.banenor.dto.CameraPose;
import com.banenor.dto.DigitalTwinDTO;
import com.banenor.dto.SensorMetricsDTO;
import com.banenor.model.DigitalTwin;
import com.banenor.repository.DigitalTwinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalTwinServiceImpl implements DigitalTwinService {

    private final DigitalTwinRepository repository;

    @Override
    public Mono<Void> updateTwin(SensorMetricsDTO metrics) {
        DigitalTwin entity = DigitalTwin.builder()
                .assetId(metrics.getAnalysisId())
                .recordedAt(metrics.getCreatedAt())
                .metricType("averageSpeed")
                .metricValue(metrics.getAverageSpeed())
                .componentName("aggregate")
                .location(null)
                .status("UPDATED")
                .riskScore(metrics.getRiskScore() != null ? metrics.getRiskScore() : 0.0)
                .build();

        log.debug("[DigitalTwinService] Saving new twin record for asset {}", metrics.getAnalysisId());
        return repository.save(entity)
                .doOnSuccess(e -> log.info("[DigitalTwinService] Saved twin record id={} for asset {}", e.getId(), metrics.getAnalysisId()))
                .doOnError(err -> log.error("[DigitalTwinService] Failed to save twin record for asset {}", metrics.getAnalysisId(), err))
                .then();
    }

    @Override
    public Mono<CameraPose> getTwinState(Integer assetId) {
        return repository.findTopByAssetIdOrderByRecordedAtDesc(assetId)
                .map(this::toPose)
                .doOnNext(p -> log.debug("[DigitalTwinService] Retrieved pose {} for asset {}", p, assetId))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[DigitalTwinService] No twin record found for asset {}, returning default pose", assetId);
                    return Mono.just(defaultPose());
                }));
    }

    @Override
    public Flux<CameraPose> streamDigitalTwinUpdates(Integer assetId) {
        log.info("[DigitalTwinService] Starting SSE stream for asset {}", assetId);
        return Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick -> getTwinState(assetId))
                .distinctUntilChanged()
                .doOnCancel(() -> log.info("[DigitalTwinService] SSE stream cancelled for asset {}", assetId));
    }

    @Override
    public Flux<DigitalTwinDTO> findTwinsByFilters(Map<String, Object> filters, int page, int size) {
        log.debug("[DigitalTwinService] Querying twins by filters {} page={} size={}", filters, page, size);
        return repository.findByFilters(filters, page, size)
                .map(this::toDto)
                .doOnComplete(() -> log.debug("[DigitalTwinService] Completed querying twins"));
    }

    @Override
    public Mono<Long> countTwinsByFilters(Map<String, Object> filters) {
        log.debug("[DigitalTwinService] Counting twins by filters {}", filters);
        return repository.countByFilters(filters)
                .doOnNext(count -> log.debug("[DigitalTwinService] Count result = {}", count));
    }

    private CameraPose toPose(DigitalTwin e) {
        // DB does not yet store actual camera pose; use metricValue as Z fallback
        double z = e.getMetricValue() != null ? e.getMetricValue() : 0.0;
        CameraPose pose = CameraPose.builder()
                .x(0.0)
                .y(0.0)
                .z(z)
                .rx(0.0)
                .ry(0.0)
                .rz(0.0)
                .build();
        log.warn("[DigitalTwinService] Mapping DB record id={} → placeholder pose {}", e.getId(), pose);
        return pose;
    }

    private CameraPose defaultPose() {
        return CameraPose.builder()
                .x(0.0)
                .y(0.0)
                .z(0.0)
                .rx(0.0)
                .ry(0.0)
                .rz(0.0)
                .build();
    }

    private DigitalTwinDTO toDto(DigitalTwin e) {
        return DigitalTwinDTO.builder()
                .timestamp(e.getRecordedAt())
                .value(e.getMetricValue())
                .type(e.getMetricType())
                .component(e.getComponentName())
                .location(e.getLocation())
                .assetId(e.getAssetId().toString())
                .status(e.getStatus())
                .riskLevel(scoreToRisk(e.getRiskScore()))
                .build();
    }

    private DigitalTwinDTO.RiskLevel scoreToRisk(double score) {
        if (score >= 0.8) return DigitalTwinDTO.RiskLevel.HIGH;
        else if (score >= 0.5) return DigitalTwinDTO.RiskLevel.MEDIUM;
        else return DigitalTwinDTO.RiskLevel.LOW;
    }
}
