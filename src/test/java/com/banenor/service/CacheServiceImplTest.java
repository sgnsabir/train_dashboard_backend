package com.banenor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheServiceImplTest {

    private CacheServiceImpl cacheService;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager("averages");
        cacheService = new CacheServiceImpl(cacheManager);
    }

    @Test
    void testCacheAverageAndRetrieve() {
        String key = "avgSpeed";
        Double value = 85.0;
        StepVerifier.create(cacheService.cacheAverage(key, value)).verifyComplete();
        Double cachedValue = cacheService.getCachedAverage(key).block();
        assertThat(cachedValue)
                .as("Cached value should equal the stored value")
                .isEqualTo(85.0);
    }

    @Test
    void testRetrieveNonExistentKeyReturnsNull() {
        String key = "nonExistentKey";
        Double cachedValue = cacheService.getCachedAverage(key).block();
        assertThat(cachedValue)
                .as("Retrieving a non-existent key should return null")
                .isNull();
    }

    @Test
    void testOverwriteCachedValue() {
        String key = "avgSpeed";
        StepVerifier.create(cacheService.cacheAverage(key, 85.0)).verifyComplete();
        assertThat(cacheService.getCachedAverage(key).block()).isEqualTo(85.0);
        StepVerifier.create(cacheService.cacheAverage(key, 90.0)).verifyComplete();
        assertThat(cacheService.getCachedAverage(key).block())
                .as("Value should be overwritten with the new value")
                .isEqualTo(90.0);
    }

    @Test
    void testCacheMultipleKeys() {
        StepVerifier.create(cacheService.cacheAverage("avgSpeed", 85.0)).verifyComplete();
        StepVerifier.create(cacheService.cacheAverage("avgAoa", 5.0)).verifyComplete();
        StepVerifier.create(cacheService.cacheAverage("avgVibration", 3.5)).verifyComplete();

        assertThat(cacheService.getCachedAverage("avgSpeed").block()).isEqualTo(85.0);
        assertThat(cacheService.getCachedAverage("avgAoa").block()).isEqualTo(5.0);
        assertThat(cacheService.getCachedAverage("avgVibration").block()).isEqualTo(3.5);
    }

    @Test
    void testCacheThreadSafety() throws InterruptedException {
        int threadCount = 10;
        var executorService = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        var latch = new java.util.concurrent.CountDownLatch(threadCount);
        String key = "avgSpeed";

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    cacheService.cacheAverage(key, 80.0 + threadNum).block();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        Double finalValue = cacheService.getCachedAverage(key).block();
        assertThat(finalValue)
                .as("Final cached value should be between 80.0 and 89.0 after concurrent updates")
                .isBetween(80.0, 89.0);
        executorService.shutdown();
    }
}
