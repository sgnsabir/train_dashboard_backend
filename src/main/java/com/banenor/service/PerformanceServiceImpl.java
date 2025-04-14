// src/main/java/com/banenor/service/PerformanceServiceImpl.java
package com.banenor.service;

import com.banenor.dto.PerformanceDTO;
import com.banenor.model.AbstractAxles;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl implements PerformanceService {

    private final HaugfjellMP1AxlesRepository mp1Repo;
    private final HaugfjellMP3AxlesRepository mp3Repo;

    /**
     * Retrieves performance data (e.g., speed and acceleration) from sensor records within the specified date range.
     * If either start or end is null, the method defaults to the last 7 days.
     *
     * @param start the starting LocalDateTime boundary (inclusive)
     * @param end   the ending LocalDateTime boundary (inclusive)
     * @return a Flux of PerformanceDTO objects representing performance metrics.
     */
    @Override
    public Flux<PerformanceDTO> getPerformanceData(LocalDateTime start, LocalDateTime end) {
        // Compute effective boundaries as final values
        final LocalDateTime effectiveEnd = (end == null) ? LocalDateTime.now(ZoneOffset.UTC) : end;
        final LocalDateTime effectiveStart = (start == null) ? effectiveEnd.minusDays(7) : start;
        if (start == null || end == null) {
            log.debug("No date range provided; defaulting to last 7 days: start={}, end={}", effectiveStart, effectiveEnd);
        }
        log.info("Fetching performance data from {} to {}", effectiveStart, effectiveEnd);

        // Retrieve sensor records from MP1 repository and filter by created_at timestamp.
        Flux<? extends AbstractAxles> mp1Data = mp1Repo.findAll()
                .filter(axle -> isWithinRange(axle.getCreatedAt(), effectiveStart, effectiveEnd))
                .doOnNext(axle -> log.trace("MP1 sensor record: trainNo={}, createdAt={}", axle.getTrainNo(), axle.getCreatedAt()))
                .doOnError(e -> log.error("Error fetching MP1 sensor data: {}", e.getMessage(), e));

        // Retrieve sensor records from MP3 repository and filter by created_at timestamp.
        Flux<? extends AbstractAxles> mp3Data = mp3Repo.findAll()
                .filter(axle -> isWithinRange(axle.getCreatedAt(), effectiveStart, effectiveEnd))
                .doOnNext(axle -> log.trace("MP3 sensor record: trainNo={}, createdAt={}", axle.getTrainNo(), axle.getCreatedAt()))
                .doOnError(e -> log.error("Error fetching MP3 sensor data: {}", e.getMessage(), e));

        // Merge both streams and map each record to a PerformanceDTO.
        return Flux.merge(mp1Data, mp3Data)
                .map(this::mapAxleToPerformanceDTO)
                .doOnComplete(() -> log.info("Completed fetching and mapping performance data."))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Checks whether the given timestamp is within the [start, end] boundaries (inclusive).
     *
     * @param timestamp the timestamp to check
     * @param start     the start boundary
     * @param end       the end boundary
     * @return true if timestamp is within the range; false otherwise.
     */
    private boolean isWithinRange(LocalDateTime timestamp, LocalDateTime start, LocalDateTime end) {
        return (timestamp.equals(start) || timestamp.isAfter(start)) &&
                (timestamp.equals(end) || timestamp.isBefore(end));
    }

    /**
     * Maps a sensor record (AbstractAxles) to a PerformanceDTO.
     *
     * @param axle the sensor record.
     * @return the mapped PerformanceDTO.
     */
    private PerformanceDTO mapAxleToPerformanceDTO(AbstractAxles axle) {
        PerformanceDTO dto = new PerformanceDTO();
        dto.setCreatedAt(axle.getCreatedAt());
        dto.setSpeed(axle.getSpdTp1());
        // Acceleration calculation can be added in the future if needed.
        dto.setAcceleration(null);
        log.debug("Mapped Axle (ID: {}) to PerformanceDTO: speed={}, createdAt={}",
                axle.getAxleId(), dto.getSpeed(), dto.getCreatedAt());
        return dto;
    }
}
