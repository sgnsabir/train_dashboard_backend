package com.banenor.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.banenor.dto.AxlesDataDTO;
import com.banenor.model.AbstractAxles;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AxlesDataService {

    private final HaugfjellMP1AxlesRepository mp1Repository;
    private final HaugfjellMP3AxlesRepository mp3Repository;

    /**
     * Retrieves axles data for a specific train number and time range, with measurement point type.
     *
     * @param trainNo the train number
     * @param start the start time
     * @param end the end time
     * @param measurementPoint optional measurement point type ("MP1" or "MP3")
     * @return Flux of AxlesDataDTO
     */
    public Flux<AxlesDataDTO> getAxlesData(Integer trainNo, LocalDateTime start, LocalDateTime end, String measurementPoint) {
        if (measurementPoint != null && !measurementPoint.isEmpty()) {
            return getAxlesDataByType(trainNo, start, end, measurementPoint);
        }
        
        // If no measurement point specified, return data from both
        return Flux.merge(
            getAxlesDataByType(trainNo, start, end, "MP1"),
            getAxlesDataByType(trainNo, start, end, "MP3")
        );
    }

    private Flux<AxlesDataDTO> getAxlesDataByType(Integer trainNo, LocalDateTime start, LocalDateTime end, String type) {
        return switch (type.toUpperCase()) {
            case "MP1" -> mp1Repository.findByTrainNoAndCreatedAtBetween(trainNo, start, end)
                    .map(axle -> mapToDTO(axle, "MP1"));
            case "MP3" -> mp3Repository.findByTrainNoAndCreatedAtBetween(trainNo, start, end)
                    .map(axle -> mapToDTO(axle, "MP3"));
            default -> Flux.error(new IllegalArgumentException("Invalid measurement point type: " + type));
        };
    }

    /**
     * Gets global aggregation data for a specific measurement point type.
     *
     * @param type the measurement point type ("MP1" or "MP3")
     * @return Mono containing the aggregated data
     */
    public Mono<AxlesDataDTO> getGlobalAggregations(String type) {
        return switch (type.toUpperCase()) {
            case "MP1" -> Mono.zip(
                List.of(
                    mp1Repository.findGlobalAverageSpeed(),
                    mp1Repository.findGlobalAverageAoa(),
                    mp1Repository.findGlobalAverageVibrationLeft(),
                    mp1Repository.findGlobalAverageVibrationRight(),
                    mp1Repository.findGlobalAverageVerticalForceLeft(),
                    mp1Repository.findGlobalAverageVerticalForceRight(),
                    mp1Repository.findGlobalAverageLateralForceLeft(),
                    mp1Repository.findGlobalAverageLateralForceRight(),
                    mp1Repository.findGlobalAverageLateralVibrationLeft(),
                    mp1Repository.findGlobalAverageLateralVibrationRight()
                ),
                results -> buildAggregationDTO(Arrays.stream(results)
                    .map(obj -> (Double) obj)
                    .collect(Collectors.toList()), "MP1")
            );
            case "MP3" -> Mono.zip(
                List.of(
                    mp3Repository.findGlobalAverageSpeed(),
                    mp3Repository.findGlobalAverageAoa(),
                    mp3Repository.findGlobalAverageVibrationLeft(),
                    mp3Repository.findGlobalAverageVibrationRight(),
                    mp3Repository.findGlobalAverageVerticalForceLeft(),
                    mp3Repository.findGlobalAverageVerticalForceRight(),
                    mp3Repository.findGlobalAverageLateralForceLeft(),
                    mp3Repository.findGlobalAverageLateralForceRight(),
                    mp3Repository.findGlobalAverageLateralVibrationLeft(),
                    mp3Repository.findGlobalAverageLateralVibrationRight()
                ),
                results -> buildAggregationDTO(Arrays.stream(results)
                    .map(obj -> (Double) obj)
                    .collect(Collectors.toList()), "MP3")
            );
            default -> Mono.error(new IllegalArgumentException("Invalid measurement point type: " + type));
        };
    }

    private AxlesDataDTO buildAggregationDTO(List<Double> results, String type) {
        return AxlesDataDTO.builder()
                .speed(results.get(0))
                .angleOfAttack(results.get(1))
                .vibrationLeft(results.get(2))
                .vibrationRight(results.get(3))
                .verticalForceLeft(results.get(4))
                .verticalForceRight(results.get(5))
                .lateralForceLeft(results.get(6))
                .lateralForceRight(results.get(7))
                .lateralVibrationLeft(results.get(8))
                .lateralVibrationRight(results.get(9))
                .measurementPoint(type)
                .build();
    }

    private AxlesDataDTO mapToDTO(AbstractAxles axle, String measurementPoint) {
        return AxlesDataDTO.builder()
                .trainNo(axle.getHeader() != null ? axle.getHeader().getTrainNo() : null)
                .speed(axle.getSpdTp1())
                .angleOfAttack(axle.getAoaTp1())
                .vibrationLeft(axle.getVviblTp1())
                .vibrationRight(axle.getVvibrTp1())
                .verticalForceLeft(axle.getVfrclTp1())
                .verticalForceRight(axle.getVfrcrTp1())
                .lateralForceLeft(axle.getLfrclTp1())
                .lateralForceRight(axle.getLfrcrTp1())
                .lateralVibrationLeft(axle.getLviblTp1())
                .lateralVibrationRight(axle.getLvibrTp1())
                .createdAt(axle.getCreatedAt())
                .measurementPoint(measurementPoint)
                .build();
    }
} 
