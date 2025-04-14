package com.banenor.repository;

import com.banenor.model.HaugfjellMP1Axles;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

public interface HaugfjellMP1AxlesRepository extends R2dbcRepository<HaugfjellMP1Axles, Integer> {

    @Query("SELECT * FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Flux<HaugfjellMP1Axles> findByTrainNo(Integer trainNo);

    @Query("""
           SELECT * FROM haugfjell_mp1_axles
           WHERE train_no = :trainNo
             AND created_at >= :start
             AND created_at <= :end
           ORDER BY created_at
           """)
    Flux<HaugfjellMP1Axles> findByTrainNoAndCreatedAtBetween(Integer trainNo, LocalDateTime start, LocalDateTime end);

    // Speed Aggregations
    @Query("SELECT AVG(spd_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageSpeedByTrainNo(Integer trainNo);

    @Query("SELECT MIN(spd_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMinSpeedByTrainNo(Integer trainNo);

    @Query("SELECT MAX(spd_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMaxSpeedByTrainNo(Integer trainNo);

    @Query("SELECT AVG(spd_tp1 * spd_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageSquareSpeedByTrainNo(Integer trainNo);

    default Mono<Double> findSpeedVarianceByTrainNo(Integer trainNo) {
        return findAverageSpeedByTrainNo(trainNo)
                .zipWith(findAverageSquareSpeedByTrainNo(trainNo),
                        (avg, avgSq) -> (avg != null && avgSq != null) ? avgSq - (avg * avg) : null);
    }

    // Vibration Aggregations (Left)
    @Query("SELECT AVG(vvibl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageVibrationLeftByTrainNo(Integer trainNo);

    @Query("SELECT MIN(vvibl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMinVibrationLeftByTrainNo(Integer trainNo);

    @Query("SELECT MAX(vvibl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMaxVibrationLeftByTrainNo(Integer trainNo);

    @Query("SELECT AVG(vvibl_tp1 * vvibl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageSquareVibrationLeftByTrainNo(Integer trainNo);

    // Vibration Aggregations (Right)
    @Query("SELECT AVG(vvibr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageVibrationRightByTrainNo(Integer trainNo);

    @Query("SELECT MIN(vvibr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMinVibrationRightByTrainNo(Integer trainNo);

    @Query("SELECT MAX(vvibr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMaxVibrationRightByTrainNo(Integer trainNo);

    @Query("SELECT AVG(vvibr_tp1 * vvibr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageSquareVibrationRightByTrainNo(Integer trainNo);

    // Vibration Aggregations (using left as representative)
    @Query("SELECT AVG(vvibl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageVibrationByTrainNo(Integer trainNo);

    @Query("SELECT MIN(vvibl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMinVibrationByTrainNo(Integer trainNo);

    @Query("SELECT MAX(vvibl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMaxVibrationByTrainNo(Integer trainNo);

    @Query("SELECT AVG(vvibl_tp1 * vvibl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageSquareVibrationByTrainNo(Integer trainNo);

    // Vertical Force Left Aggregations
    @Query("SELECT MIN(vfrcl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMinVerticalForceLeftByTrainNo(Integer trainNo);

    @Query("SELECT MAX(vfrcl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMaxVerticalForceLeftByTrainNo(Integer trainNo);

    @Query("SELECT AVG(vfrcl_tp1 * vfrcl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageSquareVerticalForceLeftByTrainNo(Integer trainNo);

    // Vertical Force Right Aggregations
    @Query("SELECT MIN(vfrcr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMinVerticalForceRightByTrainNo(Integer trainNo);

    @Query("SELECT MAX(vfrcr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMaxVerticalForceRightByTrainNo(Integer trainNo);

    @Query("SELECT AVG(vfrcr_tp1 * vfrcr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageSquareVerticalForceRightByTrainNo(Integer trainNo);

    // Angle of Attack Aggregations
    @Query("SELECT AVG(aoa_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageAoaByTrainNo(Integer trainNo);

    @Query("SELECT MIN(aoa_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMinAoaByTrainNo(Integer trainNo);

    @Query("SELECT MAX(aoa_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMaxAoaByTrainNo(Integer trainNo);

    @Query("SELECT AVG(aoa_tp1 * aoa_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageSquareAoaByTrainNo(Integer trainNo);

    // Axle Load Aggregations
    @Query("SELECT AVG(vfrcl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageAxleLoadLeftByTrainNo(Integer trainNo);

    @Query("SELECT AVG(vfrcr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageAxleLoadRightByTrainNo(Integer trainNo);

    // Lateral Force Aggregations (Left)
    @Query("SELECT AVG(lfrcl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageLateralForceLeftByTrainNo(Integer trainNo);

    @Query("SELECT MIN(lfrcl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMinLateralForceLeftByTrainNo(Integer trainNo);

    @Query("SELECT MAX(lfrcl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMaxLateralForceLeftByTrainNo(Integer trainNo);

    @Query("SELECT AVG(lfrcl_tp1 * lfrcl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageSquareLateralForceLeftByTrainNo(Integer trainNo);

    // Lateral Force Aggregations (Right)
    @Query("SELECT AVG(lfrcr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageLateralForceRightByTrainNo(Integer trainNo);

    @Query("SELECT MIN(lfrcr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMinLateralForceRightByTrainNo(Integer trainNo);

    @Query("SELECT MAX(lfrcr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMaxLateralForceRightByTrainNo(Integer trainNo);

    @Query("SELECT AVG(lfrcr_tp1 * lfrcr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageSquareLateralForceRightByTrainNo(Integer trainNo);

    // Lateral Vibration Aggregations (Left)
    @Query("SELECT AVG(lvibl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageLateralVibrationLeftByTrainNo(Integer trainNo);

    @Query("SELECT MIN(lvibl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMinLateralVibrationLeftByTrainNo(Integer trainNo);

    @Query("SELECT MAX(lvibl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMaxLateralVibrationLeftByTrainNo(Integer trainNo);

    @Query("SELECT AVG(lvibl_tp1 * lvibl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageSquareLateralVibrationLeftByTrainNo(Integer trainNo);

    // Lateral Vibration Aggregations (Right)
    @Query("SELECT AVG(lvibr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageLateralVibrationRightByTrainNo(Integer trainNo);

    @Query("SELECT MIN(lvibr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMinLateralVibrationRightByTrainNo(Integer trainNo);

    @Query("SELECT MAX(lvibr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findMaxLateralVibrationRightByTrainNo(Integer trainNo);

    @Query("SELECT AVG(lvibr_tp1 * lvibr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Mono<Double> findAverageSquareLateralVibrationRightByTrainNo(Integer trainNo);

    // Latest Sensor Readings (ordered by created_at DESC)
    @Query("SELECT spd_tp1 FROM haugfjell_mp1_axles WHERE train_no = :trainNo ORDER BY created_at DESC LIMIT 1")
    Mono<Double> findFirstSpdTp1ByTrainNoOrderByCreatedAtDesc(Integer trainNo);

    @Query("SELECT vvibl_tp1 FROM haugfjell_mp1_axles WHERE train_no = :trainNo ORDER BY created_at DESC LIMIT 1")
    Mono<Double> findFirstVviblTp1ByTrainNoOrderByCreatedAtDesc(Integer trainNo);

    @Query("SELECT vvibr_tp1 FROM haugfjell_mp1_axles WHERE train_no = :trainNo ORDER BY created_at DESC LIMIT 1")
    Mono<Double> findFirstVvibrTp1ByTrainNoOrderByCreatedAtDesc(Integer trainNo);

    @Query("SELECT vfrcr_tp1 FROM haugfjell_mp1_axles WHERE train_no = :trainNo ORDER BY created_at DESC LIMIT 1")
    Mono<Double> findFirstVfrcrTp1ByTrainNoOrderByCreatedAtDesc(Integer trainNo);

    @Query("SELECT lfrcr_tp1 FROM haugfjell_mp1_axles WHERE train_no = :trainNo ORDER BY created_at DESC LIMIT 1")
    Mono<Double> findFirstLfrcrTp1ByTrainNoOrderByCreatedAtDesc(Integer trainNo);

    @Query("SELECT lvibr_tp1 FROM haugfjell_mp1_axles WHERE train_no = :trainNo ORDER BY created_at DESC LIMIT 1")
    Mono<Double> findFirstLvibrTp1ByTrainNoOrderByCreatedAtDesc(Integer trainNo);

    @Query("SELECT vfrcl_tp1 FROM haugfjell_mp1_axles WHERE train_no = :trainNo ORDER BY created_at DESC LIMIT 1")
    Mono<Double> findFirstVfrclTp1ByTrainNoOrderByCreatedAtDesc(Integer trainNo);

    @Query("SELECT vfrcr_tp1 FROM haugfjell_mp1_axles WHERE train_no = :trainNo ORDER BY created_at DESC LIMIT 1")
    Mono<Double> findFirstAxleLoadRightByTrainNoOrderByCreatedAtDesc(Integer trainNo);

    // Global Aggregations (across all records)
    @Query("SELECT AVG(spd_tp1) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageSpeed();

    @Query("SELECT AVG(spd_tp1 * spd_tp1) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageSquareSpeed();

    @Query("SELECT AVG(aoa_tp1) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageAoa();

    @Query("SELECT AVG(vvibl_tp1) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageVibrationLeft();

    @Query("SELECT AVG(vvibr_tp1) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageVibrationRight();

    @Query("SELECT AVG(vfrcl_tp1) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageAxleLoadLeft();

    @Query("SELECT AVG(vfrcr_tp1) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageAxleLoadRight();

    @Query("SELECT AVG(lfrcl_tp1) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageLateralForceLeft();

    @Query("SELECT AVG(lfrcr_tp1) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageLateralForceRight();

    @Query("SELECT AVG(lvibl_tp1) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageLateralVibrationLeft();

    @Query("SELECT AVG(lvibr_tp1) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageLateralVibrationRight();

    @Query("SELECT AVG(vfrcl_tp1) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageVerticalForceLeft();

    @Query("SELECT AVG(vfrcr_tp1) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageVerticalForceRight();

    // Vertical Force by Train Number
    @Query("SELECT AVG(vfrcl_tp1) FROM haugfjell_mp1_axles WHERE train_no = :analysisId")
    Mono<Double> findAverageVerticalForceLeftByTrainNo(Integer analysisId);

    @Query("SELECT AVG(vfrcr_tp1) FROM haugfjell_mp1_axles WHERE train_no = :analysisId")
    Mono<Double> findAverageVerticalForceRightByTrainNo(Integer analysisId);
}
