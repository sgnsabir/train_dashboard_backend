package com.banenor.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.banenor.dto.SensorMetricsDTO;
import com.banenor.dto.DigitalTwinDTO;
import com.banenor.model.DigitalTwin;
import com.banenor.repository.DigitalTwinRepository;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class DigitalTwinServiceImpl implements DigitalTwinService {

    private final DigitalTwinRepository digitalTwinRepository;

    public DigitalTwinServiceImpl(DigitalTwinRepository digitalTwinRepository) {
        this.digitalTwinRepository = digitalTwinRepository;
    }

    /**
     * Updates the digital twin state for a given asset persistently.
     * This implementation uses a reactive R2DBC repository to store the twin state.
     *
     * @param metrics aggregated sensor metrics; must include a non-null analysisId as the asset identifier.
     * @return a Mono signaling completion.
     */
    @Override
    public Mono<Void> updateTwin(SensorMetricsDTO metrics) {
        if (metrics == null) {
            log.warn("Received null sensor metrics for digital twin update.");
            return Mono.empty();
        }
        Integer assetId = metrics.getAnalysisId(); // Using analysisId as the asset identifier
        if (assetId == null) {
            log.warn("Sensor metrics missing asset identifier.");
            return Mono.error(new IllegalArgumentException("Asset identifier is missing in sensor metrics."));
        }
        // Build a sensor summary string from key metrics (adjust as needed for your domain)
        String sensorSummary = String.format("Speed: %.2f km/h, AOA: %.2f, Vibration: %.2f",
                metrics.getAverageSpeed(),
                metrics.getAverageAoa(),
                metrics.getAverageVibration());

        // Retrieve existing twin from the persistent store, or create a new one if not present.
        return digitalTwinRepository.findById(assetId)
                .defaultIfEmpty(DigitalTwin.builder().assetId(assetId).build())
                .flatMap(existingTwin -> {
                    existingTwin.setStatus("Operational"); // In a real scenario, compute the status dynamically.
                    existingTwin.setUpdatedAt(LocalDateTime.now());
                    existingTwin.setSensorSummary(sensorSummary);
                    return digitalTwinRepository.save(existingTwin);
                })
                .doOnNext(updatedTwin -> log.info("Updated digital twin for asset {}: {}", assetId, updatedTwin))
                .then();
    }

    /**
     * Retrieves the current persistent digital twin state for the specified asset.
     *
     * @param assetId the asset identifier (e.g., train number).
     * @return a Mono emitting the VirtualAssetDTO representing the current digital twin state.
     */
    @Override
    public Mono<DigitalTwinDTO> getTwinState(Integer assetId) {
        if (assetId == null) {
            return Mono.error(new IllegalArgumentException("Asset identifier cannot be null."));
        }
        return digitalTwinRepository.findById(assetId)
                .map(twin -> DigitalTwinDTO.builder()
                        .assetId(twin.getAssetId())
                        .status(twin.getStatus())
                        .updatedAt(twin.getUpdatedAt())
                        .sensorSummary(twin.getSensorSummary())
                        .build())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Digital twin for asset " + assetId + " not found.")));
    }

    @Override
    public Flux<DigitalTwinDTO> findTwinsByFilters(Map<String, Object> filters, int page, int size) {
        log.debug("Filtering DigitalTwins: {} (page={}, size={})", filters, page, size);
        return digitalTwinRepository
                .findByFilters(filters, page, size)
                .map(this::toDto);  // convert DigitalTwin → VirtualAssetDTO
    }

    @Override
    public Mono<Long> countTwinsByFilters(Map<String, Object> filters) {
        log.debug("Counting DigitalTwins for filters: {}", filters);
        return digitalTwinRepository.countByFilters(filters);
    }

    private DigitalTwinDTO toDto(DigitalTwin twin) {
        return DigitalTwinDTO.builder()
                .assetId(twin.getAssetId())
                .status(twin.getStatus())
                .sensorSummary(twin.getSensorSummary())
                .updatedAt(twin.getUpdatedAt())
                .build();
    }
}
