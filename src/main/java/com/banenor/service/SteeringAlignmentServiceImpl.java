package com.banenor.service;

import com.banenor.config.InsightsProperties;
import com.banenor.dto.SteeringAlignmentDTO;
import com.banenor.model.AbstractAxles;
import com.banenor.model.HaugfjellMP1Header;
import com.banenor.model.HaugfjellMP3Header;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.repository.HaugfjellMP3HeaderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('MAINTENANCE')")
public class SteeringAlignmentServiceImpl implements SteeringAlignmentService {

    // Inject concrete repositories instead of an abstract repository resolver.
    private final HaugfjellMP1HeaderRepository mp1HeaderRepository;
    private final HaugfjellMP3HeaderRepository mp3HeaderRepository;
    private final HaugfjellMP1AxlesRepository mp1Repository;
    private final HaugfjellMP3AxlesRepository mp3Repository;
    private final InsightsProperties insightsProperties;

    @Override
    public Flux<SteeringAlignmentDTO> fetchSteeringData(Integer trainNo, LocalDateTime start, LocalDateTime end) {
        // Try MP1 branch
        Mono<Flux<AbstractAxles>> mp1FluxMono = mp1HeaderRepository.findById(trainNo)
                .flatMap(header -> Mono.just(mp1Repository.findByTrainNoAndCreatedAtBetween(trainNo, start, end).cast(AbstractAxles.class)));
        // Try MP3 branch
        Mono<Flux<AbstractAxles>> mp3FluxMono = mp3HeaderRepository.findById(trainNo)
                .flatMap(header -> Mono.just(mp3Repository.findByTrainNoAndCreatedAtBetween(trainNo, start, end).cast(AbstractAxles.class)));

        // Merge results from both branches (if any)
        return Mono.zip(
                        mp1FluxMono.defaultIfEmpty(Flux.empty()),
                        mp3FluxMono.defaultIfEmpty(Flux.empty())
                )
                .flatMapMany(tuple -> Flux.merge(tuple.getT1(), tuple.getT2()))
                .map(record -> analyzeRecord(trainNo, record))
                .doOnError(ex -> log.error("Error in steering alignment analysis: {}", ex.getMessage(), ex));
    }

    /**
     * Analyzes a sensor record for steering alignment issues.
     *
     * Checks:
     * - If the absolute angle of attack (from aoa_tp1) exceeds the configured threshold.
     * - Computes the lateral-to-vertical force ratio (using lateral forces and vertical forces)
     *   compared against the configured threshold.
     *
     * @param trainNo the train number as provided to the controller.
     * @param record  a sensor record.
     * @return a populated SteeringAlignmentDTO with the analysis results.
     */
    private SteeringAlignmentDTO analyzeRecord(Integer trainNo, AbstractAxles record) {
        double angleOfAttack = (record.getAoaTp1() != null) ? Math.abs(record.getAoaTp1()) : 0.0;
        boolean aoaOutOfSpec = angleOfAttack > insightsProperties.getSteering().getAoaThreshold();

        double lateralForceLeft = (record.getLfrclTp1() != null) ? Math.abs(record.getLfrclTp1()) : 0.0;
        double lateralForceRight = (record.getLfrcrTp1() != null) ? Math.abs(record.getLfrcrTp1()) : 0.0;
        double verticalForceLeft = (record.getVfrclTp1() != null) ? record.getVfrclTp1() : 0.0;
        double verticalForceRight = (record.getVfrcrTp1() != null) ? record.getVfrcrTp1() : 0.0;
        double totalVerticalForce = verticalForceLeft + verticalForceRight;

        double lateralVerticalRatio = 0.0;
        if (totalVerticalForce > 0) {
            lateralVerticalRatio = (lateralForceLeft + lateralForceRight) / totalVerticalForce;
        }
        boolean misalignmentDetected = lateralVerticalRatio > insightsProperties.getSteering().getLvRatioThreshold();

        StringBuilder anomalyMessage = new StringBuilder();
        if (aoaOutOfSpec) {
            anomalyMessage.append(String.format("Angle of attack (%.2f) exceeds threshold (%.2f). ",
                    angleOfAttack, insightsProperties.getSteering().getAoaThreshold()));
        }
        if (misalignmentDetected) {
            anomalyMessage.append(String.format("Lateral/Vertical force ratio (%.2f) exceeds threshold (%.2f). ",
                    lateralVerticalRatio, insightsProperties.getSteering().getLvRatioThreshold()));
        }
        if (!aoaOutOfSpec && !misalignmentDetected) {
            anomalyMessage.append("Steering parameters are within acceptable ranges.");
        }

        Integer resolvedTrainNo = (record.getHeader() != null) ? record.getHeader().getTrainNo() : trainNo;
        LocalDateTime measurementTime = record.getCreatedAt();

        return SteeringAlignmentDTO.builder()
                .trainNo(resolvedTrainNo)
                .measurementTime(measurementTime)
                .angleOfAttack(angleOfAttack)
                .lateralForceLeft(lateralForceLeft)
                .lateralForceRight(lateralForceRight)
                .verticalForceLeft(verticalForceLeft)
                .verticalForceRight(verticalForceRight)
                .lateralVerticalRatio(lateralVerticalRatio)
                .aoaOutOfSpec(aoaOutOfSpec)
                .misalignmentDetected(misalignmentDetected)
                .anomalyMessage(anomalyMessage.toString().trim())
                .build();
    }
}
