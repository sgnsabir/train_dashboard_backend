// src/main/java/com/banenor/service/DigitalTwinServiceImpl.java
package com.banenor.service;

import com.banenor.dto.DigitalTwinDTO;
import com.banenor.dto.SensorMetricsDTO;
import com.banenor.model.DigitalTwin;
import com.banenor.repository.DigitalTwinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

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
                .location(null)  // no start/end on SensorMetricsDTO
                .status("UPDATED")
                .riskScore(
                        metrics.getRiskScore() != null
                                ? metrics.getRiskScore()
                                : 0.0
                )
                .build();
        return repository.save(entity).then();
    }

    @Override
    public Mono<DigitalTwinDTO> getTwinState(Integer assetId) {
        return repository
                .findTopByAssetIdOrderByRecordedAtDesc(assetId)
                .map(this::toDto);
    }

    @Override
    public Flux<DigitalTwinDTO> findTwinsByFilters(Map<String, Object> filters, int page, int size) {
        return repository
                .findByFilters(filters, page, size)
                .map(this::toDto);
    }

    @Override
    public Mono<Long> countTwinsByFilters(Map<String, Object> filters) {
        return repository.countByFilters(filters);
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
        if (score >= 0.8)      return DigitalTwinDTO.RiskLevel.HIGH;
        else if (score >= 0.5) return DigitalTwinDTO.RiskLevel.MEDIUM;
        else                   return DigitalTwinDTO.RiskLevel.LOW;
    }
}
