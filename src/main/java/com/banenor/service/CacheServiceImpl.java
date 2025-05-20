package com.banenor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
public class CacheServiceImpl implements CacheService {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    private final ReactiveValueOperations<String, Object> valueOps;

    public CacheServiceImpl(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.valueOps = redisTemplate.opsForValue();
    }

    @Override
    public Mono<Void> cacheAverage(String key, Double value) {
        String redisKey = "averages:" + key;
        return valueOps
                .set(redisKey, value, DEFAULT_TTL)
                .doOnSuccess(ok -> {
                    if (Boolean.TRUE.equals(ok)) {
                        log.debug("Cached average {} = {} (TTL {}s)", redisKey, value, DEFAULT_TTL.getSeconds());
                    } else {
                        log.warn("Failed to cache average for key {}", redisKey);
                    }
                })
                .doOnError(ex -> log.error("Error caching average for key {}", redisKey, ex))
                .then();
    }

    @Override
    public Mono<Double> getCachedAverage(String key) {
        String redisKey = "averages:" + key;
        return valueOps
                .get(redisKey)
                .cast(Double.class)
                .doOnNext(val -> log.debug("Fetched cached average {} = {}", redisKey, val))
                .onErrorResume(ex -> {
                    log.error("Error retrieving cached average for key {}", redisKey, ex);
                    return Mono.just(0.0);
                })
                .defaultIfEmpty(0.0);
    }
}
