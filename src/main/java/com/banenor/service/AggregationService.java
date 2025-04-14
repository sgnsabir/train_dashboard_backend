package com.banenor.service;

import reactor.core.publisher.Mono;

public interface AggregationService {

    // --- Speed Aggregations ---
    Mono<Double> getAverageSpeed(Integer trainNo);
    Mono<Double> getMinSpeed(Integer trainNo);
    Mono<Double> getMaxSpeed(Integer trainNo);
    Mono<Double> getSpeedVariance(Integer trainNo);

    // --- Angle of Attack Aggregations ---
    Mono<Double> getAverageAoa(Integer trainNo);
    Mono<Double> getMinAoa(Integer trainNo);
    Mono<Double> getMaxAoa(Integer trainNo);
    Mono<Double> getAoaVariance(Integer trainNo);

    // --- Vibration Aggregations (Left) ---
    Mono<Double> getAverageVibrationLeft(Integer trainNo);
    Mono<Double> getMinVibrationLeft(Integer trainNo);
    Mono<Double> getMaxVibrationLeft(Integer trainNo);
    Mono<Double> getVibrationLeftVariance(Integer trainNo);

    // --- Vibration Aggregations (Right) ---
    Mono<Double> getAverageVibrationRight(Integer trainNo);
    Mono<Double> getMinVibrationRight(Integer trainNo);
    Mono<Double> getMaxVibrationRight(Integer trainNo);
    Mono<Double> getVibrationRightVariance(Integer trainNo);

    // --- Vertical Force Aggregations (Left) ---
    Mono<Double> getAverageVerticalForceLeft(Integer trainNo);
    Mono<Double> getMinVerticalForceLeft(Integer trainNo);
    Mono<Double> getMaxVerticalForceLeft(Integer trainNo);
    Mono<Double> getVerticalForceLeftVariance(Integer trainNo);

    // --- Vertical Force Aggregations (Right) ---
    Mono<Double> getAverageVerticalForceRight(Integer trainNo);
    Mono<Double> getMinVerticalForceRight(Integer trainNo);
    Mono<Double> getMaxVerticalForceRight(Integer trainNo);
    Mono<Double> getVerticalForceRightVariance(Integer trainNo);

    // --- Lateral Force Aggregations (Left) ---
    Mono<Double> getAverageLateralForceLeft(Integer trainNo);
    Mono<Double> getMinLateralForceLeft(Integer trainNo);
    Mono<Double> getMaxLateralForceLeft(Integer trainNo);
    Mono<Double> getLateralForceLeftVariance(Integer trainNo);

    // --- Lateral Force Aggregations (Right) ---
    Mono<Double> getAverageLateralForceRight(Integer trainNo);
    Mono<Double> getMinLateralForceRight(Integer trainNo);
    Mono<Double> getMaxLateralForceRight(Integer trainNo);
    Mono<Double> getLateralForceRightVariance(Integer trainNo);

    // --- Lateral Vibration Aggregations (Left) ---
    Mono<Double> getAverageLateralVibrationLeft(Integer trainNo);
    Mono<Double> getMinLateralVibrationLeft(Integer trainNo);
    Mono<Double> getMaxLateralVibrationLeft(Integer trainNo);
    Mono<Double> getLateralVibrationLeftVariance(Integer trainNo);

    // --- Lateral Vibration Aggregations (Right) ---
    Mono<Double> getAverageLateralVibrationRight(Integer trainNo);
    Mono<Double> getMinLateralVibrationRight(Integer trainNo);
    Mono<Double> getMaxLateralVibrationRight(Integer trainNo);
    Mono<Double> getLateralVibrationRightVariance(Integer trainNo);
}
