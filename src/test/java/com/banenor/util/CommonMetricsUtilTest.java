package com.banenor.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommonMetricsUtil Tests")
class CommonMetricsUtilTest {

    @Test
    @DisplayName("combineMetrics returns average when both values are non-null")
    void testCombineMetrics_BothNonNull() {
        Double combined = CommonMetricsUtil.combineMetrics(10.0, 20.0);
        assertEquals(15.0, combined, "Average of 10.0 and 20.0 should be 15.0");
    }

    @Test
    @DisplayName("combineMetrics treats null as 0.0 for first value")
    void testCombineMetrics_OneNull_FirstNull() {
        Double combined = CommonMetricsUtil.combineMetrics(null, 20.0);
        assertEquals(10.0, combined, "When first value is null, result should be (0.0+20.0)/2=10.0");
    }

    @Test
    @DisplayName("combineMetrics treats null as 0.0 for second value")
    void testCombineMetrics_OneNull_SecondNull() {
        Double combined = CommonMetricsUtil.combineMetrics(10.0, null);
        assertEquals(5.0, combined, "When second value is null, result should be (10.0+0.0)/2=5.0");
    }

    @Test
    @DisplayName("combineMetrics returns 0.0 when both values are null")
    void testCombineMetrics_BothNull() {
        Double combined = CommonMetricsUtil.combineMetrics(null, null);
        assertEquals(0.0, combined, "When both values are null, result should be 0.0");
    }

    @Test
    @DisplayName("combineMetrics is symmetric")
    void testCombineMetrics_Symmetric() {
        Double result1 = CommonMetricsUtil.combineMetrics(5.0, 15.0);
        Double result2 = CommonMetricsUtil.combineMetrics(15.0, 5.0);
        assertEquals(result1, result2, "combineMetrics should be symmetric");
    }

    @Test
    @DisplayName("combineMetrics computes correct average with zeros")
    void testCombineMetrics_WithZeros() {
        Double combinedZeros = CommonMetricsUtil.combineMetrics(0.0, 0.0);
        assertEquals(0.0, combinedZeros, "Average of two zeros should be zero");

        Double combined = CommonMetricsUtil.combineMetrics(0.0, 10.0);
        assertEquals(5.0, combined, "Average of 0.0 and 10.0 should be 5.0");
    }

    @Test
    @DisplayName("combineMetrics computes correct average with negative numbers")
    void testCombineMetrics_WithNegativeNumbers() {
        Double combined = CommonMetricsUtil.combineMetrics(-10.0, 10.0);
        assertEquals(0.0, combined, "Average of -10.0 and 10.0 should be 0.0");

        Double combinedNegatives = CommonMetricsUtil.combineMetrics(-20.0, -10.0);
        assertEquals(-15.0, combinedNegatives, "Average of -20.0 and -10.0 should be -15.0");
    }

    @ParameterizedTest(name = "combineMetrics({0}, {1}) = {2}")
    @CsvSource({
            "5.0, 15.0, 10.0",
            "100.0, 200.0, 150.0",
            "0.0, 0.0, 0.0",
            "-10.0, 10.0, 0.0"
    })
    @DisplayName("Parameterized tests for combineMetrics")
    void parameterizedTest_CombineMetrics(Double val1, Double val2, Double expected) {
        Double result = CommonMetricsUtil.combineMetrics(val1, val2);
        assertEquals(expected, result, () -> "Expected average of " + val1 + " and " + val2 + " to be " + expected);
    }

    @Test
    @DisplayName("calculateVariance returns correct value for normal case")
    void testCalculateVariance_NormalCase() {
        Double variance = CommonMetricsUtil.calculateVariance(10.0, 110.0);
        assertEquals(10.0, variance, "Variance should be 10.0 for average=10 and averageSquare=110");
    }

    @Test
    @DisplayName("calculateVariance returns averageSquare when average is zero")
    void testCalculateVariance_ZeroAverage() {
        Double variance = CommonMetricsUtil.calculateVariance(0.0, 25.0);
        assertEquals(25.0, variance, "Variance with zero average should equal the averageSquare value");
    }

    @Test
    @DisplayName("calculateVariance works with negative average")
    void testCalculateVariance_WithNegativeAverage() {
        Double variance = CommonMetricsUtil.calculateVariance(-5.0, 50.0);
        assertEquals(25.0, variance, "Variance should be computed correctly even for negative average values");
    }

    @Test
    @DisplayName("calculateVariance treats null average as 0.0")
    void testCalculateVariance_NullAverage() {
        Double variance = CommonMetricsUtil.calculateVariance(null, 50.0);
        assertEquals(50.0, variance, "Null average should be treated as 0.0 resulting in variance 50.0");
    }

    @Test
    @DisplayName("calculateVariance treats null averageSquare as 0.0")
    void testCalculateVariance_NullAverageSquare() {
        Double variance = CommonMetricsUtil.calculateVariance(10.0, null);
        assertEquals(-100.0, variance, "Null averageSquare should be treated as 0.0 resulting in variance -100.0");
    }

    @Test
    @DisplayName("calculateVariance returns 0.0 when both inputs are null")
    void testCalculateVariance_BothNull() {
        Double variance = CommonMetricsUtil.calculateVariance(null, null);
        assertEquals(0.0, variance, "Both null inputs should be treated as 0.0, resulting in variance 0.0");
    }

    @Test
    @DisplayName("calculateVariance can return negative value if averageSquare is insufficient")
    void testCalculateVariance_InsufficientSquare() {
        Double variance = CommonMetricsUtil.calculateVariance(10.0, 50.0);
        assertEquals(-50.0, variance, "Variance can be negative if averageSquare is less than the square of average");
    }
}
