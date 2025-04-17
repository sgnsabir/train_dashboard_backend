package com.banenor.repository;

import com.banenor.model.HaugfjellMP3Axles;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface HaugfjellMP3AxlesRepository extends R2dbcRepository<HaugfjellMP3Axles, Integer> {

 // Legacy Queries (using TP1 only)
 @Query("SELECT * FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
 Flux<HaugfjellMP3Axles> findByTrainNo(Integer trainNo);

 @Query("""
           SELECT * FROM haugfjell_mp3_axles
           WHERE train_no = :trainNo
             AND created_at >= :start
             AND created_at <= :end
           ORDER BY created_at
           """)
 Flux<HaugfjellMP3Axles> findByTrainNoAndCreatedAtBetween(Integer trainNo, LocalDateTime start, LocalDateTime end);

 // Speed Aggregations (using TP1 only)
 @Query("SELECT AVG(spd_tp1) FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
 Mono<Double> findAverageSpeedByTrainNo(Integer trainNo);

 @Query("SELECT MIN(spd_tp1) FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
 Mono<Double> findMinSpeedByTrainNo(Integer trainNo);

 @Query("SELECT MAX(spd_tp1) FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
 Mono<Double> findMaxSpeedByTrainNo(Integer trainNo);

 @Query("SELECT AVG(spd_tp1 * spd_tp1) FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
 Mono<Double> findAverageSquareSpeedByTrainNo(Integer trainNo);

 default Mono<Double> findSpeedVarianceByTrainNo(Integer trainNo) {
  return findAverageSpeedByTrainNo(trainNo)
          .zipWith(findAverageSquareSpeedByTrainNo(trainNo),
                  (avg, avgSq) -> (avg != null && avgSq != null) ? avgSq - (avg * avg) : null);
 }

 //////////////// DYNAMIC AGGREGATION QUERIES (GROUP BY vit) ////////////////

 // 1. Speed Dynamic Aggregations (using spd_tp1, spd_tp2, spd_tp3, spd_tp5, spd_tp6, spd_tp8)
 @Query("""
           SELECT vit,
                  AVG((spd_tp1 + spd_tp2 + spd_tp3 + spd_tp5 + spd_tp6 + spd_tp8) / 6.0) AS avg_speed,
                  MIN(LEAST(spd_tp1, spd_tp2, spd_tp3, spd_tp5, spd_tp6, spd_tp8)) AS min_speed,
                  MAX(GREATEST(spd_tp1, spd_tp2, spd_tp3, spd_tp5, spd_tp6, spd_tp8)) AS max_speed,
                  AVG((spd_tp1 * spd_tp1 + spd_tp2 * spd_tp2 + spd_tp3 * spd_tp3 + spd_tp5 * spd_tp5 + spd_tp6 * spd_tp6 + spd_tp8 * spd_tp8) / 6.0) AS avg_square_speed
           FROM haugfjell_mp3_axles
           WHERE train_no = :trainNo
           GROUP BY vit
           """)
 Flux<SpeedDynamicAggregation> findDynamicSpeedAggregationsByTrainNo(Integer trainNo);

 interface SpeedDynamicAggregation {
  String getVit();
  Double getAvgSpeed();
  Double getMinSpeed();
  Double getMaxSpeed();
  Double getAvgSquareSpeed();
 }

 // 2. Angle of Attack Dynamic Aggregations (using aoa_tp1, aoa_tp2, aoa_tp3, aoa_tp5, aoa_tp6, aoa_tp8)
 @Query("""
           SELECT vit,
                  AVG((aoa_tp1 + aoa_tp2 + aoa_tp3 + aoa_tp5 + aoa_tp6 + aoa_tp8) / 6.0) AS avg_aoa,
                  MIN(LEAST(aoa_tp1, aoa_tp2, aoa_tp3, aoa_tp5, aoa_tp6, aoa_tp8)) AS min_aoa,
                  MAX(GREATEST(aoa_tp1, aoa_tp2, aoa_tp3, aoa_tp5, aoa_tp6, aoa_tp8)) AS max_aoa,
                  AVG((aoa_tp1 * aoa_tp1 + aoa_tp2 * aoa_tp2 + aoa_tp3 * aoa_tp3 + aoa_tp5 * aoa_tp5 + aoa_tp6 * aoa_tp6 + aoa_tp8 * aoa_tp8) / 6.0) AS avg_square_aoa
           FROM haugfjell_mp3_axles
           WHERE train_no = :trainNo
           GROUP BY vit
           """)
 Flux<AngleDynamicAggregation> findDynamicAngleAggregationsByTrainNo(Integer trainNo);

 interface AngleDynamicAggregation {
  String getVit();
  Double getAvgAoa();
  Double getMinAoa();
  Double getMaxAoa();
  Double getAvgSquareAoa();
 }

 // 3. Vertical Vibration (Left) Dynamic Aggregations (using vvibl_tp1, vvibl_tp2, vvibl_tp3, vvibl_tp5, vvibl_tp6, vvibl_tp8)
 @Query("""
           SELECT vit,
                  AVG((vvibl_tp1 + vvibl_tp2 + vvibl_tp3 + vvibl_tp5 + vvibl_tp6 + vvibl_tp8) / 6.0) AS avg_vibration_left,
                  MIN(LEAST(vvibl_tp1, vvibl_tp2, vvibl_tp3, vvibl_tp5, vvibl_tp6, vvibl_tp8)) AS min_vibration_left,
                  MAX(GREATEST(vvibl_tp1, vvibl_tp2, vvibl_tp3, vvibl_tp5, vvibl_tp6, vvibl_tp8)) AS max_vibration_left,
                  AVG((vvibl_tp1 * vvibl_tp1 + vvibl_tp2 * vvibl_tp2 + vvibl_tp3 * vvibl_tp3 + vvibl_tp5 * vvibl_tp5 + vvibl_tp6 * vvibl_tp6 + vvibl_tp8 * vvibl_tp8) / 6.0) AS avg_square_vibration_left
           FROM haugfjell_mp3_axles
           WHERE train_no = :trainNo
           GROUP BY vit
           """)
 Flux<VibrationLeftDynamicAggregation> findDynamicVibrationLeftAggregationsByTrainNo(Integer trainNo);

 interface VibrationLeftDynamicAggregation {
  String getVit();
  Double getAvgVibrationLeft();
  Double getMinVibrationLeft();
  Double getMaxVibrationLeft();
  Double getAvgSquareVibrationLeft();
 }

 // 4. Vertical Vibration (Right) Dynamic Aggregations (using vvibr_tp1, vvibr_tp2, vvibr_tp3, vvibr_tp5, vvibr_tp6, vvibr_tp8)
 @Query("""
           SELECT vit,
                  AVG((vvibr_tp1 + vvibr_tp2 + vvibr_tp3 + vvibr_tp5 + vvibr_tp6 + vvibr_tp8) / 6.0) AS avg_vibration_right,
                  MIN(LEAST(vvibr_tp1, vvibr_tp2, vvibr_tp3, vvibr_tp5, vvibr_tp6, vvibr_tp8)) AS min_vibration_right,
                  MAX(GREATEST(vvibr_tp1, vvibr_tp2, vvibr_tp3, vvibr_tp5, vvibr_tp6, vvibr_tp8)) AS max_vibration_right,
                  AVG((vvibr_tp1 * vvibr_tp1 + vvibr_tp2 * vvibr_tp2 + vvibr_tp3 * vvibr_tp3 + vvibr_tp5 * vvibr_tp5 + vvibr_tp6 * vvibr_tp6 + vvibr_tp8 * vvibr_tp8) / 6.0) AS avg_square_vibration_right
           FROM haugfjell_mp3_axles
           WHERE train_no = :trainNo
           GROUP BY vit
           """)
 Flux<VibrationRightDynamicAggregation> findDynamicVibrationRightAggregationsByTrainNo(Integer trainNo);

 interface VibrationRightDynamicAggregation {
  String getVit();
  Double getAvgVibrationRight();
  Double getMinVibrationRight();
  Double getMaxVibrationRight();
  Double getAvgSquareVibrationRight();
 }

 // 5. Vertical Force (Left) Dynamic Aggregations (using vfrcl_tp1, vfrcl_tp2, vfrcl_tp3, vfrcl_tp5, vfrcl_tp6, vfrcl_tp8)
 @Query("""
           SELECT vit,
                  AVG((vfrcl_tp1 + vfrcl_tp2 + vfrcl_tp3 + vfrcl_tp5 + vfrcl_tp6 + vfrcl_tp8) / 6.0) AS avg_vertical_force_left,
                  MIN(LEAST(vfrcl_tp1, vfrcl_tp2, vfrcl_tp3, vfrcl_tp5, vfrcl_tp6, vfrcl_tp8)) AS min_vertical_force_left,
                  MAX(GREATEST(vfrcl_tp1, vfrcl_tp2, vfrcl_tp3, vfrcl_tp5, vfrcl_tp6, vfrcl_tp8)) AS max_vertical_force_left,
                  AVG((vfrcl_tp1 * vfrcl_tp1 + vfrcl_tp2 * vfrcl_tp2 + vfrcl_tp3 * vfrcl_tp3 + vfrcl_tp5 * vfrcl_tp5 + vfrcl_tp6 * vfrcl_tp6 + vfrcl_tp8 * vfrcl_tp8) / 6.0) AS avg_square_vertical_force_left
           FROM haugfjell_mp3_axles
           WHERE train_no = :trainNo
           GROUP BY vit
           """)
 Flux<VerticalForceLeftDynamicAggregation> findDynamicVerticalForceLeftAggregationsByTrainNo(Integer trainNo);

 interface VerticalForceLeftDynamicAggregation {
  String getVit();
  Double getAvgVerticalForceLeft();
  Double getMinVerticalForceLeft();
  Double getMaxVerticalForceLeft();
  Double getAvgSquareVerticalForceLeft();
 }

 // 6. Vertical Force (Right) Dynamic Aggregations (using vfrcr_tp1, vfrcr_tp2, vfrcr_tp3, vfrcr_tp5, vfrcr_tp6, vfrcr_tp8)
 @Query("""
           SELECT vit,
                  AVG((vfrcr_tp1 + vfrcr_tp2 + vfrcr_tp3 + vfrcr_tp5 + vfrcr_tp6 + vfrcr_tp8) / 6.0) AS avg_vertical_force_right,
                  MIN(LEAST(vfrcr_tp1, vfrcr_tp2, vfrcr_tp3, vfrcr_tp5, vfrcr_tp6, vfrcr_tp8)) AS min_vertical_force_right,
                  MAX(GREATEST(vfrcr_tp1, vfrcr_tp2, vfrcr_tp3, vfrcr_tp5, vfrcr_tp6, vfrcr_tp8)) AS max_vertical_force_right,
                  AVG((vfrcr_tp1 * vfrcr_tp1 + vfrcr_tp2 * vfrcr_tp2 + vfrcr_tp3 * vfrcr_tp3 + vfrcr_tp5 * vfrcr_tp5 + vfrcr_tp6 * vfrcr_tp6 + vfrcr_tp8 * vfrcr_tp8) / 6.0) AS avg_square_vertical_force_right
           FROM haugfjell_mp3_axles
           WHERE train_no = :trainNo
           GROUP BY vit
           """)
 Flux<VerticalForceRightDynamicAggregation> findDynamicVerticalForceRightAggregationsByTrainNo(Integer trainNo);

 interface VerticalForceRightDynamicAggregation {
  String getVit();
  Double getAvgVerticalForceRight();
  Double getMinVerticalForceRight();
  Double getMaxVerticalForceRight();
  Double getAvgSquareVerticalForceRight();
 }

 // 7. Lateral Force (Left) Dynamic Aggregations (using lfrcl_tp1, lfrcl_tp2, lfrcl_tp3, lfrcl_tp5, lfrcl_tp6)
 @Query("""
           SELECT vit,
                  AVG((lfrcl_tp1 + lfrcl_tp2 + lfrcl_tp3 + lfrcl_tp5 + lfrcl_tp6) / 5.0) AS avg_lateral_force_left,
                  MIN(LEAST(lfrcl_tp1, lfrcl_tp2, lfrcl_tp3, lfrcl_tp5, lfrcl_tp6)) AS min_lateral_force_left,
                  MAX(GREATEST(lfrcl_tp1, lfrcl_tp2, lfrcl_tp3, lfrcl_tp5, lfrcl_tp6)) AS max_lateral_force_left,
                  AVG((lfrcl_tp1 * lfrcl_tp1 + lfrcl_tp2 * lfrcl_tp2 + lfrcl_tp3 * lfrcl_tp3 + lfrcl_tp5 * lfrcl_tp5 + lfrcl_tp6 * lfrcl_tp6) / 5.0) AS avg_square_lateral_force_left
           FROM haugfjell_mp3_axles
           WHERE train_no = :trainNo
           GROUP BY vit
           """)
 Flux<LateralForceLeftDynamicAggregation> findDynamicLateralForceLeftAggregationsByTrainNo(Integer trainNo);

 interface LateralForceLeftDynamicAggregation {
  String getVit();
  Double getAvgLateralForceLeft();
  Double getMinLateralForceLeft();
  Double getMaxLateralForceLeft();
  Double getAvgSquareLateralForceLeft();
 }

 // 8. Lateral Force (Right) Dynamic Aggregations (using lfrcr_tp1, lfrcr_tp2, lfrcr_tp3, lfrcr_tp5, lfrcr_tp6)
 @Query("""
           SELECT vit,
                  AVG((lfrcr_tp1 + lfrcr_tp2 + lfrcr_tp3 + lfrcr_tp5 + lfrcr_tp6) / 5.0) AS avg_lateral_force_right,
                  MIN(LEAST(lfrcr_tp1, lfrcr_tp2, lfrcr_tp3, lfrcr_tp5, lfrcr_tp6)) AS min_lateral_force_right,
                  MAX(GREATEST(lfrcr_tp1, lfrcr_tp2, lfrcr_tp3, lfrcr_tp5, lfrcr_tp6)) AS max_lateral_force_right,
                  AVG((lfrcr_tp1 * lfrcr_tp1 + lfrcr_tp2 * lfrcr_tp2 + lfrcr_tp3 * lfrcr_tp3 + lfrcr_tp5 * lfrcr_tp5 + lfrcr_tp6 * lfrcr_tp6) / 5.0) AS avg_square_lateral_force_right
           FROM haugfjell_mp3_axles
           WHERE train_no = :trainNo
           GROUP BY vit
           """)
 Flux<LateralForceRightDynamicAggregation> findDynamicLateralForceRightAggregationsByTrainNo(Integer trainNo);

 interface LateralForceRightDynamicAggregation {
  String getVit();
  Double getAvgLateralForceRight();
  Double getMinLateralForceRight();
  Double getMaxLateralForceRight();
  Double getAvgSquareLateralForceRight();
 }

 // 9. Lateral Vibration (Left) Dynamic Aggregations (using lvibl_tp1, lvibl_tp2, lvibl_tp3, lvibl_tp5, lvibl_tp6)
 @Query("""
           SELECT vit,
                  AVG((lvibl_tp1 + lvibl_tp2 + lvibl_tp3 + lvibl_tp5 + lvibl_tp6) / 5.0) AS avg_lateral_vibration_left,
                  MIN(LEAST(lvibl_tp1, lvibl_tp2, lvibl_tp3, lvibl_tp5, lvibl_tp6)) AS min_lateral_vibration_left,
                  MAX(GREATEST(lvibl_tp1, lvibl_tp2, lvibl_tp3, lvibl_tp5, lvibl_tp6)) AS max_lateral_vibration_left,
                  AVG((lvibl_tp1 * lvibl_tp1 + lvibl_tp2 * lvibl_tp2 + lvibl_tp3 * lvibl_tp3 + lvibl_tp5 * lvibl_tp5 + lvibl_tp6 * lvibl_tp6) / 5.0) AS avg_square_lateral_vibration_left
           FROM haugfjell_mp3_axles
           WHERE train_no = :trainNo
           GROUP BY vit
           """)
 Flux<LateralVibrationLeftDynamicAggregation> findDynamicLateralVibrationLeftAggregationsByTrainNo(Integer trainNo);

 interface LateralVibrationLeftDynamicAggregation {
  String getVit();
  Double getAvgLateralVibrationLeft();
  Double getMinLateralVibrationLeft();
  Double getMaxLateralVibrationLeft();
  Double getAvgSquareLateralVibrationLeft();
 }

 // 10. Lateral Vibration (Right) Dynamic Aggregations (using lvibr_tp1, lvibr_tp2, lvibr_tp3, lvibr_tp5, lvibr_tp6)
 @Query("""
           SELECT vit,
                  AVG((lvibr_tp1 + lvibr_tp2 + lvibr_tp3 + lvibr_tp5 + lvibr_tp6) / 5.0) AS avg_lateral_vibration_right,
                  MIN(LEAST(lvibr_tp1, lvibr_tp2, lvibr_tp3, lvibr_tp5, lvibr_tp6)) AS min_lateral_vibration_right,
                  MAX(GREATEST(lvibr_tp1, lvibr_tp2, lvibr_tp3, lvibr_tp5, lvibr_tp6)) AS max_lateral_vibration_right,
                  AVG((lvibr_tp1 * lvibr_tp1 + lvibr_tp2 * lvibr_tp2 + lvibr_tp3 * lvibr_tp3 + lvibr_tp5 * lvibr_tp5 + lvibr_tp6 * lvibr_tp6) / 5.0) AS avg_square_lateral_vibration_right
           FROM haugfjell_mp3_axles
           WHERE train_no = :trainNo
           GROUP BY vit
           """)
 Flux<LateralVibrationRightDynamicAggregation> findDynamicLateralVibrationRightAggregationsByTrainNo(Integer trainNo);

 interface LateralVibrationRightDynamicAggregation {
  String getVit();
  Double getAvgLateralVibrationRight();
  Double getMinLateralVibrationRight();
  Double getMaxLateralVibrationRight();
  Double getAvgSquareLateralVibrationRight();
 }

 //////////////// GLOBAL AGGREGATION QUERIES (using all available TP columns) ////////////////

 // Global Average Speed (using spd_tp1, spd_tp2, spd_tp3, spd_tp5, spd_tp6, spd_tp8)
 @Query("SELECT AVG((spd_tp1 + spd_tp2 + spd_tp3 + spd_tp5 + spd_tp6 + spd_tp8) / 6.0) FROM haugfjell_mp3_axles")
 Mono<Double> findGlobalAverageSpeed();

 // Global Average Square Speed
 @Query("SELECT AVG((spd_tp1 * spd_tp1 + spd_tp2 * spd_tp2 + spd_tp3 * spd_tp3 + spd_tp5 * spd_tp5 + spd_tp6 * spd_tp6 + spd_tp8 * spd_tp8) / 6.0) FROM haugfjell_mp3_axles")
 Mono<Double> findGlobalAverageSquareSpeed();

 // Global Average Angle of Attack (using aoa_tp1, aoa_tp2, aoa_tp3, aoa_tp5, aoa_tp6, aoa_tp8)
 @Query("SELECT AVG((aoa_tp1 + aoa_tp2 + aoa_tp3 + aoa_tp5 + aoa_tp6 + aoa_tp8) / 6.0) FROM haugfjell_mp3_axles")
 Mono<Double> findGlobalAverageAoa();

 // Global Average Vertical Vibration Left (using vvibl_tp1, vvibl_tp2, vvibl_tp3, vvibl_tp5, vvibl_tp6, vvibl_tp8)
 @Query("SELECT AVG((vvibl_tp1 + vvibl_tp2 + vvibl_tp3 + vvibl_tp5 + vvibl_tp6 + vvibl_tp8) / 6.0) FROM haugfjell_mp3_axles")
 Mono<Double> findGlobalAverageVibrationLeft();

 // Global Average Vertical Vibration Right (using vvibr_tp1, vvibr_tp2, vvibr_tp3, vvibr_tp5, vvibr_tp6, vvibr_tp8)
 @Query("SELECT AVG((vvibr_tp1 + vvibr_tp2 + vvibr_tp3 + vvibr_tp5 + vvibr_tp6 + vvibr_tp8) / 6.0) FROM haugfjell_mp3_axles")
 Mono<Double> findGlobalAverageVibrationRight();

 // Global Average Axle Load Left (using vfrcl_tp1, vfrcl_tp2, vfrcl_tp3, vfrcl_tp5, vfrcl_tp6, vfrcl_tp8)
 @Query("SELECT AVG((vfrcl_tp1 + vfrcl_tp2 + vfrcl_tp3 + vfrcl_tp5 + vfrcl_tp6 + vfrcl_tp8) / 6.0) FROM haugfjell_mp3_axles")
 Mono<Double> findGlobalAverageAxleLoadLeft();

 // Global Average Axle Load Right (using vfrcr_tp1, vfrcr_tp2, vfrcr_tp3, vfrcr_tp5, vfrcr_tp6, vfrcr_tp8)
 @Query("SELECT AVG((vfrcr_tp1 + vfrcr_tp2 + vfrcr_tp3 + vfrcr_tp5 + vfrcr_tp6 + vfrcr_tp8) / 6.0) FROM haugfjell_mp3_axles")
 Mono<Double> findGlobalAverageAxleLoadRight();

 // Global Average Lateral Force Left (using lfrcl_tp1, lfrcl_tp2, lfrcl_tp3, lfrcl_tp5, lfrcl_tp6)
 @Query("SELECT AVG((lfrcl_tp1 + lfrcl_tp2 + lfrcl_tp3 + lfrcl_tp5 + lfrcl_tp6) / 5.0) FROM haugfjell_mp3_axles")
 Mono<Double> findGlobalAverageLateralForceLeft();

 // Global Average Lateral Force Right (using lfrcr_tp1, lfrcr_tp2, lfrcr_tp3, lfrcr_tp5, lfrcr_tp6)
 @Query("SELECT AVG((lfrcr_tp1 + lfrcr_tp2 + lfrcr_tp3 + lfrcr_tp5 + lfrcr_tp6) / 5.0) FROM haugfjell_mp3_axles")
 Mono<Double> findGlobalAverageLateralForceRight();

 // Global Average Lateral Vibration Left (using lvibl_tp1, lvibl_tp2, lvibl_tp3, lvibl_tp5, lvibl_tp6)
 @Query("SELECT AVG((lvibl_tp1 + lvibl_tp2 + lvibl_tp3 + lvibl_tp5 + lvibl_tp6) / 5.0) FROM haugfjell_mp3_axles")
 Mono<Double> findGlobalAverageLateralVibrationLeft();

 // Global Average Lateral Vibration Right (using lvibr_tp1, lvibr_tp2, lvibr_tp3, lvibr_tp5, lvibr_tp6)
 @Query("SELECT AVG((lvibr_tp1 + lvibr_tp2 + lvibr_tp3 + lvibr_tp5 + lvibr_tp6) / 5.0) FROM haugfjell_mp3_axles")
 Mono<Double> findGlobalAverageLateralVibrationRight();

 // Global Average Vertical Force Left (using vfrcl_tp1, vfrcl_tp2, vfrcl_tp3, vfrcl_tp5, vfrcl_tp6, vfrcl_tp8)
 @Query("SELECT AVG((vfrcl_tp1 + vfrcl_tp2 + vfrcl_tp3 + vfrcl_tp5 + vfrcl_tp6 + vfrcl_tp8) / 6.0) FROM haugfjell_mp3_axles")
 Mono<Double> findGlobalAverageVerticalForceLeft();

 // Global Average Vertical Force Right (using vfrcr_tp1, vfrcr_tp2, vfrcr_tp3, vfrcr_tp5, vfrcr_tp6, vfrcr_tp8)
 @Query("SELECT AVG((vfrcr_tp1 + vfrcr_tp2 + vfrcr_tp3 + vfrcr_tp5 + vfrcr_tp6 + vfrcr_tp8) / 6.0) FROM haugfjell_mp3_axles")
 Mono<Double> findGlobalAverageVerticalForceRight();

 // Global Vertical Force by Train Number (using all vertical force left/right TPs)
 @Query("SELECT AVG((vfrcl_tp1 + vfrcl_tp2 + vfrcl_tp3 + vfrcl_tp5 + vfrcl_tp6 + vfrcl_tp8)/6.0) FROM haugfjell_mp3_axles WHERE train_no = :analysisId")
 Mono<Double> findAverageVerticalForceLeftByTrainNo(Integer analysisId);

 @Query("SELECT AVG((vfrcr_tp1 + vfrcr_tp2 + vfrcr_tp3 + vfrcr_tp5 + vfrcr_tp6 + vfrcr_tp8)/6.0) FROM haugfjell_mp3_axles WHERE train_no = :analysisId")
 Mono<Double> findAverageVerticalForceRightByTrainNo(Integer analysisId);
}
