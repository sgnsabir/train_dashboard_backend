package com.banenor.util;

public class CommonMetricsUtil {

    /**
     * Combines two Double values by computing their average.
     * If a value is null, it is treated as 0.0.
     *
     * @param value1 the first value, may be null.
     * @param value2 the second value, may be null.
     * @return the average of value1 and value2, treating null as 0.0.
     */
    public static Double combineMetrics(Double value1, Double value2) {
        double v1 = (value1 == null) ? 0.0 : value1;
        double v2 = (value2 == null) ? 0.0 : value2;
        return (v1 + v2) / 2.0;
    }

    /**
     * Calculates the variance using the formula:
     * variance = averageSquare - (average)^2.
     * If an input is null, it is treated as 0.0.
     *
     * @param average the average value, may be null.
     * @param averageSquare the average of squared values, may be null.
     * @return the computed variance.
     */
    public static Double calculateVariance(Double average, Double averageSquare) {
        double avg = (average == null) ? 0.0 : average;
        double avgSq = (averageSquare == null) ? 0.0 : averageSquare;
        return avgSq - (avg * avg);
    }
}
