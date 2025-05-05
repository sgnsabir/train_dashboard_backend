package com.banenor.repository;

import com.banenor.model.HaugfjellMP1Axles;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface HaugfjellMP1AxlesRepository extends R2dbcRepository<HaugfjellMP1Axles, Integer> {

    // ──────────────────────────────────────────────────────────────────────────
    // 1. RAW DATA ACCESS
    // ──────────────────────────────────────────────────────────────────────────

    // 1.1 Fetch all records for a train
    @Query("SELECT * FROM haugfjell_mp1_axles WHERE train_no = :trainNo")
    Flux<HaugfjellMP1Axles> findByTrainNo(Integer trainNo);

    // 1.2 Fetch a time-window for a train
    @Query("""
            SELECT * FROM haugfjell_mp1_axles
            WHERE train_no = :trainNo
              AND created_at >= :start
              AND created_at <= :end
            ORDER BY created_at
            """)
    Flux<HaugfjellMP1Axles> findByTrainNoAndCreatedAtBetween(
            Integer trainNo,
            LocalDateTime start,
            LocalDateTime end
    );


    // ──────────────────────────────────────────────────────────────────────────
    // 2. DYNAMIC AGGREGATIONS (GROUP BY vit)
    //    → For detailed charts of metric vs. VIT
    // ──────────────────────────────────────────────────────────────────────────

    // 2.1 Speed
    @Query("""
            SELECT vit,
                   AVG((spd_tp1 + spd_tp2 + spd_tp3 + spd_tp5 + spd_tp6 + spd_tp8)/6.0)    AS avg_speed,
                   MIN(LEAST(spd_tp1,spd_tp2,spd_tp3,spd_tp5,spd_tp6,spd_tp8))             AS min_speed,
                   MAX(GREATEST(spd_tp1,spd_tp2,spd_tp3,spd_tp5,spd_tp6,spd_tp8))           AS max_speed,
                   AVG((spd_tp1*spd_tp1 + spd_tp2*spd_tp2 + spd_tp3*spd_tp3 +
                        spd_tp5*spd_tp5 + spd_tp6*spd_tp6 + spd_tp8*spd_tp8)/6.0)         AS avg_square_speed
            FROM haugfjell_mp1_axles
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

    // 2.2 Angle of Attack
    @Query("""
            SELECT vit,
                   AVG((aoa_tp1 + aoa_tp2 + aoa_tp3 + aoa_tp5 + aoa_tp6 + aoa_tp8)/6.0)    AS avg_aoa,
                   MIN(LEAST(aoa_tp1,aoa_tp2,aoa_tp3,aoa_tp5,aoa_tp6,aoa_tp8))             AS min_aoa,
                   MAX(GREATEST(aoa_tp1,aoa_tp2,aoa_tp3,aoa_tp5,aoa_tp6,aoa_tp8))           AS max_aoa,
                   AVG((aoa_tp1*aoa_tp1 + aoa_tp2*aoa_tp2 + aoa_tp3*aoa_tp3 +
                        aoa_tp5*aoa_tp5 + aoa_tp6*aoa_tp6 + aoa_tp8*aoa_tp8)/6.0)         AS avg_square_aoa
            FROM haugfjell_mp1_axles
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

    // 2.3 Vertical Vibration (Left)
    @Query("""
            SELECT vit,
                   AVG((vvibl_tp1 + vvibl_tp2 + vvibl_tp3 + vvibl_tp5 + vvibl_tp6 + vvibl_tp8)/6.0)    AS avg_vibration_left,
                   MIN(LEAST(vvibl_tp1,vvibl_tp2,vvibl_tp3,vvibl_tp5,vvibl_tp6,vvibl_tp8))             AS min_vibration_left,
                   MAX(GREATEST(vvibl_tp1,vvibl_tp2,vvibl_tp3,vvibl_tp5,vvibl_tp6,vvibl_tp8))           AS max_vibration_left,
                   AVG((vvibl_tp1*vvibl_tp1 + vvibl_tp2*vvibl_tp2 + vvibl_tp3*vvibl_tp3 +
                        vvibl_tp5*vvibl_tp5 + vvibl_tp6*vvibl_tp6 + vvibl_tp8*vvibl_tp8)/6.0)         AS avg_square_vibration_left
            FROM haugfjell_mp1_axles
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

    // 2.4 Vertical Vibration (Right)
    @Query("""
            SELECT vit,
                   AVG((vvibr_tp1 + vvibr_tp2 + vvibr_tp3 + vvibr_tp5 + vvibr_tp6 + vvibr_tp8)/6.0)    AS avg_vibration_right,
                   MIN(LEAST(vvibr_tp1,vvibr_tp2,vvibr_tp3,vvibr_tp5,vvibr_tp6,vvibr_tp8))             AS min_vibration_right,
                   MAX(GREATEST(vvibr_tp1,vvibr_tp2,vvibr_tp3,vvibr_tp5,vvibr_tp6,vvibr_tp8))           AS max_vibration_right,
                   AVG((vvibr_tp1*vvibr_tp1 + vvibr_tp2*vvibr_tp2 + vvibr_tp3*vvibr_tp3 +
                        vvibr_tp5*vvibr_tp5 + vvibr_tp6*vvibr_tp6 + vvibr_tp8*vvibr_tp8)/6.0)         AS avg_square_vibration_right
            FROM haugfjell_mp1_axles
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

    // 2.5 Vertical Force (Left)
    @Query("""
            SELECT vit,
                   AVG((vfrcl_tp1 + vfrcl_tp2 + vfrcl_tp3 + vfrcl_tp5 + vfrcl_tp6 + vfrcl_tp8)/6.0)    AS avg_vertical_force_left,
                   MIN(LEAST(vfrcl_tp1,vfrcl_tp2,vfrcl_tp3,vfrcl_tp5,vfrcl_tp6,vfrcl_tp8))             AS min_vertical_force_left,
                   MAX(GREATEST(vfrcl_tp1,vfrcl_tp2,vfrcl_tp3,vfrcl_tp5,vfrcl_tp6,vfrcl_tp8))           AS max_vertical_force_left,
                   AVG((vfrcl_tp1*vfrcl_tp1 + vfrcl_tp2*vfrcl_tp2 + vfrcl_tp3*vfrcl_tp3 +
                        vfrcl_tp5*vfrcl_tp5 + vfrcl_tp6*vfrcl_tp6 + vfrcl_tp8*vfrcl_tp8)/6.0)         AS avg_square_vertical_force_left
            FROM haugfjell_mp1_axles
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

    // 2.6 Vertical Force (Right)
    @Query("""
            SELECT vit,
                   AVG((vfrcr_tp1 + vfrcr_tp2 + vfrcr_tp3 + vfrcr_tp5 + vfrcr_tp6 + vfrcr_tp8)/6.0)    AS avg_vertical_force_right,
                   MIN(LEAST(vfrcr_tp1,vfrcr_tp2,vfrcr_tp3,vfrcr_tp5,vfrcr_tp6,vfrcr_tp8))             AS min_vertical_force_right,
                   MAX(GREATEST(vfrcr_tp1,vfrcr_tp2,vfrcr_tp3,vfrcr_tp5,vfrcr_tp6,vfrcr_tp8))           AS max_vertical_force_right,
                   AVG((vfrcr_tp1*vfrcr_tp1 + vfrcr_tp2*vfrcr_tp2 + vfrcr_tp3*vfrcr_tp3 +
                        vfrcr_tp5*vfrcr_tp5 + vfrcr_tp6*vfrcr_tp6 + vfrcr_tp8*vfrcr_tp8)/6.0)         AS avg_square_vertical_force_right
            FROM haugfjell_mp1_axles
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

    // 2.7 Lateral Force (Left)
    @Query("""
            SELECT vit,
                   AVG((lfrcl_tp1 + lfrcl_tp2 + lfrcl_tp3 + lfrcl_tp5 + lfrcl_tp6)/5.0)    AS avg_lateral_force_left,
                   MIN(LEAST(lfrcl_tp1,lfrcl_tp2,lfrcl_tp3,lfrcl_tp5,lfrcl_tp6))             AS min_lateral_force_left,
                   MAX(GREATEST(lfrcl_tp1,lfrcl_tp2,lfrcl_tp3,lfrcl_tp5,lfrcl_tp6))           AS max_lateral_force_left,
                   AVG((lfrcl_tp1*lfrcl_tp1 + lfrcl_tp2*lfrcl_tp2 + lfrcl_tp3*lfrcl_tp3 +
                        lfrcl_tp5*lfrcl_tp5 + lfrcl_tp6*lfrcl_tp6)/5.0)                     AS avg_square_lateral_force_left
            FROM haugfjell_mp1_axles
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

    // 2.8 Lateral Force (Right)
    @Query("""
            SELECT vit,
                   AVG((lfrcr_tp1 + lfrcr_tp2 + lfrcr_tp3 + lfrcr_tp5 + lfrcr_tp6)/5.0)    AS avg_lateral_force_right,
                   MIN(LEAST(lfrcr_tp1,lfrcr_tp2,lfrcr_tp3,lfrcr_tp5,lfrcr_tp6))             AS min_lateral_force_right,
                   MAX(GREATEST(lfrcr_tp1,lfrcr_tp2,lfrcr_tp3,lfrcr_tp5,lfrcr_tp6))           AS max_lateral_force_right,
                   AVG((lfrcr_tp1*lfrcr_tp1 + lfrcr_tp2*lfrcr_tp2 + lfrcr_tp3*lfrcr_tp3 +
                        lfrcr_tp5*lfrcr_tp5 + lfrcr_tp6*lfrcr_tp6)/5.0)                     AS avg_square_lateral_force_right
            FROM haugfjell_mp1_axles
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

    // 2.9 Lateral Vibration (Left)
    @Query("""
            SELECT vit,
                   AVG((lvibl_tp1 + lvibl_tp2 + lvibl_tp3 + lvibl_tp5 + lvibl_tp6)/5.0)    AS avg_lateral_vibration_left,
                   MIN(LEAST(lvibl_tp1,lvibl_tp2,lvibl_tp3,lvibl_tp5,lvibl_tp6))             AS min_lateral_vibration_left,
                   MAX(GREATEST(lvibl_tp1,lvibl_tp2,lvibl_tp3,lvibl_tp5,lvibl_tp6))           AS max_lateral_vibration_left,
                   AVG((lvibl_tp1*lvibl_tp1 + lvibl_tp2*lvibl_tp2 + lvibl_tp3*lvibl_tp3 +
                        lvibl_tp5*lvibl_tp5 + lvibl_tp6*lvibl_tp6)/5.0)                     AS avg_square_lateral_vibration_left
            FROM haugfjell_mp1_axles
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

    // 2.10 Lateral Vibration (Right)
    @Query("""
            SELECT vit,
                   AVG((lvibr_tp1 + lvibr_tp2 + lvibr_tp3 + lvibr_tp5 + lvibr_tp6)/5.0)    AS avg_lateral_vibration_right,
                   MIN(LEAST(lvibr_tp1,lvibr_tp2,lvibr_tp3,lvibr_tp5,lvibr_tp6))             AS min_lateral_vibration_right,
                   MAX(GREATEST(lvibr_tp1,lvibr_tp2,lvibr_tp3,lvibr_tp5,lvibr_tp6))           AS max_lateral_vibration_right,
                   AVG((lvibr_tp1*lvibr_tp1 + lvibr_tp2*lvibr_tp2 + lvibr_tp3*lvibr_tp3 +
                        lvibr_tp5*lvibr_tp5 + lvibr_tp6*lvibr_tp6)/5.0)                     AS avg_square_lateral_vibration_right
            FROM haugfjell_mp1_axles
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

    // ──────────────────────────────────────────────────────────────────────────
    // 3. GLOBAL AGGREGATIONS (ALL TRAINS)
    //     → For site-wide KPI tiles
    // ──────────────────────────────────────────────────────────────────────────

    // 3.1 Global Average Speed
    @Query("SELECT AVG((spd_tp1+spd_tp2+spd_tp3+spd_tp5+spd_tp6+spd_tp8)/6.0) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageSpeed();

    // 3.2 Global Average Square Speed
    @Query("SELECT AVG((spd_tp1*spd_tp1 + spd_tp2*spd_tp2 + spd_tp3*spd_tp3 + spd_tp5*spd_tp5 + spd_tp6*spd_tp6 + spd_tp8*spd_tp8)/6.0) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageSquareSpeed();

    // 3.3 Global Average AOA
    @Query("SELECT AVG((aoa_tp1+aoa_tp2+aoa_tp3+aoa_tp5+aoa_tp6+aoa_tp8)/6.0) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageAoa();

    // 3.4 Global Average Vibration Left
    @Query("SELECT AVG((vvibl_tp1+vvibl_tp2+vvibl_tp3+vvibl_tp5+vvibl_tp6+vvibl_tp8)/6.0) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageVibrationLeft();

    // 3.5 Global Average Vibration Right
    @Query("SELECT AVG((vvibr_tp1+vvibr_tp2+vvibr_tp3+vvibr_tp5+vvibr_tp6+vvibr_tp8)/6.0) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageVibrationRight();

    // 3.6 Global Average Vertical Force Left
    @Query("SELECT AVG((vfrcl_tp1+vfrcl_tp2+vfrcl_tp3+vfrcl_tp5+vfrcl_tp6+vfrcl_tp8)/6.0) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageVerticalForceLeft();

    // 3.7 Global Average Vertical Force Right
    @Query("SELECT AVG((vfrcr_tp1+vfrcr_tp2+vfrcr_tp3+vfrcr_tp5+vfrcr_tp6+vfrcr_tp8)/6.0) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageVerticalForceRight();

    // 3.8 Global Average Lateral Force Left
    @Query("SELECT AVG((lfrcl_tp1+lfrcl_tp2+lfrcl_tp3+lfrcl_tp5+lfrcl_tp6)/5.0) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageLateralForceLeft();

    // 3.9 Global Average Lateral Force Right
    @Query("SELECT AVG((lfrcr_tp1+lfrcr_tp2+lfrcr_tp3+lfrcr_tp5+lfrcr_tp6)/5.0) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageLateralForceRight();

    // 3.10 Global Average Lateral Vibration Left
    @Query("SELECT AVG((lvibl_tp1+lvibl_tp2+lvibl_tp3+lvibl_tp5+lvibl_tp6)/5.0) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageLateralVibrationLeft();

    // 3.11 Global Average Lateral Vibration Right
    @Query("SELECT AVG((lvibr_tp1+lvibr_tp2+lvibr_tp3+lvibr_tp5+lvibr_tp6)/5.0) FROM haugfjell_mp1_axles")
    Mono<Double> findGlobalAverageLateralVibrationRight();

    // 4. PER-TRAIN “OVERALL” AGGREGATIONS (identical signatures to MP1, but on MP3 table)

    // 4.1 Speed
    @Query("SELECT AVG((spd_tp1+spd_tp2+spd_tp3+spd_tp5+spd_tp6+spd_tp8)/6.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgSpeedByTrainNo(Integer trainNo);

    @Query("SELECT MIN(LEAST(spd_tp1,spd_tp2,spd_tp3,spd_tp5,spd_tp6,spd_tp8)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMinSpeedByTrainNo(Integer trainNo);

    @Query("SELECT MAX(GREATEST(spd_tp1,spd_tp2,spd_tp3,spd_tp5,spd_tp6,spd_tp8)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMaxSpeedByTrainNo(Integer trainNo);

    @Query("SELECT AVG((spd_tp1*spd_tp1 + spd_tp2*spd_tp2 + spd_tp3*spd_tp3 + " +
            "spd_tp5*spd_tp5 + spd_tp6*spd_tp6 + spd_tp8*spd_tp8)/6.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgSquareSpeedByTrainNo(Integer trainNo);

    // 4.2 AOA
    @Query("SELECT AVG((aoa_tp1+aoa_tp2+aoa_tp3+aoa_tp5+aoa_tp6+aoa_tp8)/6.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgAoaByTrainNo(Integer trainNo);

    @Query("SELECT MIN(LEAST(aoa_tp1,aoa_tp2,aoa_tp3,aoa_tp5,aoa_tp6,aoa_tp8)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMinAoaByTrainNo(Integer trainNo);

    @Query("SELECT MAX(GREATEST(aoa_tp1,aoa_tp2,aoa_tp3,aoa_tp5,aoa_tp6,aoa_tp8)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMaxAoaByTrainNo(Integer trainNo);

    @Query("SELECT AVG((aoa_tp1*aoa_tp1 + aoa_tp2*aoa_tp2 + aoa_tp3*aoa_tp3 + " +
            "aoa_tp5*aoa_tp5 + aoa_tp6*aoa_tp6 + aoa_tp8*aoa_tp8)/6.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgSquareAoaByTrainNo(Integer trainNo);

    // 4.3 Vibration Left
    @Query("SELECT AVG((vvibl_tp1+vvibl_tp2+vvibl_tp3+vvibl_tp5+vvibl_tp6+vvibl_tp8)/6.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgVibrationLeftByTrainNo(Integer trainNo);

    @Query("SELECT MIN(LEAST(vvibl_tp1,vvibl_tp2,vvibl_tp3,vvibl_tp5,vvibl_tp6,vvibl_tp8)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMinVibrationLeftByTrainNo(Integer trainNo);

    @Query("SELECT MAX(GREATEST(vvibl_tp1,vvibl_tp2,vvibl_tp3,vvibl_tp5,vvibl_tp6,vvibl_tp8)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMaxVibrationLeftByTrainNo(Integer trainNo);

    @Query("SELECT AVG((vvibl_tp1*vvibl_tp1+vvibl_tp2*vvibl_tp2+vvibl_tp3*vvibl_tp3+" +
            "vvibl_tp5*vvibl_tp5+vvibl_tp6*vvibl_tp6+vvibl_tp8*vvibl_tp8)/6.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgSquareVibrationLeftByTrainNo(Integer trainNo);

    // 4.4 Vibration Right
    @Query("SELECT AVG((vvibr_tp1+vvibr_tp2+vvibr_tp3+vvibr_tp5+vvibr_tp6+vvibr_tp8)/6.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgVibrationRightByTrainNo(Integer trainNo);

    @Query("SELECT MIN(LEAST(vvibr_tp1,vvibr_tp2,vvibr_tp3,vvibr_tp5,vvibr_tp6,vvibr_tp8)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMinVibrationRightByTrainNo(Integer trainNo);

    @Query("SELECT MAX(GREATEST(vvibr_tp1,vvibr_tp2,vvibr_tp3,vvibr_tp5,vvibr_tp6,vvibr_tp8)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMaxVibrationRightByTrainNo(Integer trainNo);

    @Query("SELECT AVG((vvibr_tp1*vvibr_tp1+vvibr_tp2*vvibr_tp2+vvibr_tp3*vvibr_tp3+" +
            "vvibr_tp5*vvibr_tp5+vvibr_tp6*vvibr_tp6+vvibr_tp8*vvibr_tp8)/6.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgSquareVibrationRightByTrainNo(Integer trainNo);

    // 4.5 Vertical Force Left
    @Query("SELECT AVG((vfrcl_tp1+vfrcl_tp2+vfrcl_tp3+vfrcl_tp5+vfrcl_tp6+vfrcl_tp8)/6.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgVerticalForceLeftByTrainNo(Integer trainNo);

    @Query("SELECT MIN(LEAST(vfrcl_tp1,vfrcl_tp2,vfrcl_tp3,vfrcl_tp5,vfrcl_tp6,vfrcl_tp8)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMinVerticalForceLeftByTrainNo(Integer trainNo);

    @Query("SELECT MAX(GREATEST(vfrcl_tp1,vfrcl_tp2,vfrcl_tp3,vfrcl_tp5,vfrcl_tp6,vfrcl_tp8)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMaxVerticalForceLeftByTrainNo(Integer trainNo);

    @Query("SELECT AVG((vfrcl_tp1*vfrcl_tp1+vfrcl_tp2*vfrcl_tp2+vfrcl_tp3*vfrcl_tp3+" +
            "vfrcl_tp5*vfrcl_tp5+vfrcl_tp6*vfrcl_tp6+vfrcl_tp8*vfrcl_tp8)/6.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgSquareVerticalForceLeftByTrainNo(Integer trainNo);

    // 4.6 Vertical Force Right
    @Query("SELECT AVG((vfrcr_tp1+vfrcr_tp2+vfrcr_tp3+vfrcr_tp5+vfrcr_tp6+vfrcr_tp8)/6.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgVerticalForceRightByTrainNo(Integer trainNo);

    @Query("SELECT MIN(LEAST(vfrcr_tp1,vfrcr_tp2,vfrcr_tp3,vfrcr_tp5,vfrcr_tp6,vfrcr_tp8)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMinVerticalForceRightByTrainNo(Integer trainNo);

    @Query("SELECT MAX(GREATEST(vfrcr_tp1,vfrcr_tp2,vfrcr_tp3,vfrcr_tp5,vfrcr_tp6,vfrcr_tp8)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMaxVerticalForceRightByTrainNo(Integer trainNo);

    @Query("SELECT AVG((vfrcr_tp1*vfrcr_tp1+vfrcr_tp2*vfrcr_tp2+vfrcr_tp3*vfrcr_tp3+" +
            "vfrcr_tp5*vfrcr_tp5+vfrcr_tp6*vfrcr_tp6+vfrcr_tp8*vfrcr_tp8)/6.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgSquareVerticalForceRightByTrainNo(Integer trainNo);

    // 4.7 Lateral Force Left
    @Query("SELECT AVG((lfrcl_tp1+lfrcl_tp2+lfrcl_tp3+lfrcl_tp5+lfrcl_tp6)/5.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgLateralForceLeftByTrainNo(Integer trainNo);

    @Query("SELECT MIN(LEAST(lfrcl_tp1,lfrcl_tp2,lfrcl_tp3,lfrcl_tp5,lfrcl_tp6)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMinLateralForceLeftByTrainNo(Integer trainNo);

    @Query("SELECT MAX(GREATEST(lfrcl_tp1,lfrcl_tp2,lfrcl_tp3,lfrcl_tp5,lfrcl_tp6)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMaxLateralForceLeftByTrainNo(Integer trainNo);

    @Query("SELECT AVG((lfrcl_tp1*lfrcl_tp1 + lfrcl_tp2*lfrcl_tp2 + lfrcl_tp3*lfrcl_tp3+" +
            "lfrcl_tp5*lfrcl_tp5 + lfrcl_tp6*lfrcl_tp6)/5.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgSquareLateralForceLeftByTrainNo(Integer trainNo);

    // 4.8 Lateral Force Right
    @Query("SELECT AVG((lfrcr_tp1+lfrcr_tp2+lfrcr_tp3+lfrcr_tp5+lfrcr_tp6)/5.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgLateralForceRightByTrainNo(Integer trainNo);

    @Query("SELECT MIN(LEAST(lfrcr_tp1,lfrcr_tp2,lfrcr_tp3,lfrcr_tp5,lfrcr_tp6)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMinLateralForceRightByTrainNo(Integer trainNo);

    @Query("SELECT MAX(GREATEST(lfrcr_tp1,lfrcr_tp2,lfrcr_tp3,lfrcr_tp5,lfrcr_tp6)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMaxLateralForceRightByTrainNo(Integer trainNo);

    @Query("SELECT AVG((lfrcr_tp1*lfrcr_tp1 + lfrcr_tp2*lfrcr_tp2 + lfrcr_tp3*lfrcr_tp3+" +
            "lfrcr_tp5*lfrcr_tp5 + lfrcr_tp6*lfrcr_tp6)/5.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgSquareLateralForceRightByTrainNo(Integer trainNo);

    // 4.9 Lateral Vibration Left
    @Query("SELECT AVG((lvibl_tp1+lvibl_tp2+lvibl_tp3+lvibl_tp5+lvibl_tp6)/5.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgLateralVibrationLeftByTrainNo(Integer trainNo);

    @Query("SELECT MIN(LEAST(lvibl_tp1,lvibl_tp2,lvibl_tp3,lvibl_tp5,lvibl_tp6)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMinLateralVibrationLeftByTrainNo(Integer trainNo);

    @Query("SELECT MAX(GREATEST(lvibl_tp1,lvibl_tp2,lvibl_tp3,lvibl_tp5,lvibl_tp6)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMaxLateralVibrationLeftByTrainNo(Integer trainNo);

    @Query("SELECT AVG((lvibl_tp1*lvibl_tp1+lvibl_tp2*lvibl_tp2+lvibl_tp3*lvibl_tp3+" +
            "lvibl_tp5*lvibl_tp5+lvibl_tp6*lvibl_tp6)/5.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgSquareLateralVibrationLeftByTrainNo(Integer trainNo);

    // 4.10 Lateral Vibration Right
    @Query("SELECT AVG((lvibr_tp1+lvibr_tp2+lvibr_tp3+lvibr_tp5+lvibr_tp6)/5.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgLateralVibrationRightByTrainNo(Integer trainNo);

    @Query("SELECT MIN(LEAST(lvibr_tp1,lvibr_tp2,lvibr_tp3,lvibr_tp5,lvibr_tp6)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMinLateralVibrationRightByTrainNo(Integer trainNo);

    @Query("SELECT MAX(GREATEST(lvibr_tp1,lvibr_tp2,lvibr_tp3,lvibr_tp5,lvibr_tp6)) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallMaxLateralVibrationRightByTrainNo(Integer trainNo);

    @Query("SELECT AVG((lvibr_tp1*lvibr_tp1+lvibr_tp2*lvibr_tp2+lvibr_tp3*lvibr_tp3+" +
            "lvibr_tp5*lvibr_tp5+lvibr_tp6*lvibr_tp6)/5.0) " +
            "FROM haugfjell_mp3_axles WHERE train_no = :trainNo")
    Mono<Double> findOverallAvgSquareLateralVibrationRightByTrainNo(Integer trainNo);

}
