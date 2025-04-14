package com.banenor.service;

import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CacheServiceImpl implements CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheServiceImpl.class);
    private final CacheManager cacheManager;

    public CacheServiceImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Mono<Void> cacheAverage(String key, Double value) {
        return Mono.fromRunnable(() -> {
                    var cache = cacheManager.getCache("averages");
                    if (cache != null) {
                        cache.put(key, value);
                    } else {
                        logger.error("Cache 'averages' is not available");
                        throw new IllegalStateException("Cache 'averages' is not available");
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> {
                    if (ex instanceof RedisConnectionFailureException || ex instanceof DataAccessException) {
                        logger.error("Error caching key: {}", key, ex);
                        return Mono.<Void>empty();
                    }
                    return Mono.error(ex);
                }).then();
    }

    @Override
    public Mono<Double> getCachedAverage(String key) {
        return Mono.fromCallable(() -> {
                    var cache = cacheManager.getCache("averages");
                    if (cache != null) {
                        return cache.get(key, Double.class);
                    } else {
                        logger.error("Cache 'averages' is not available");
                        return null;
                    }
                })
                .cast(Double.class)
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> {
                    if (ex instanceof RedisConnectionFailureException || ex instanceof DataAccessException) {
                        logger.error("Error retrieving key: {}", key, ex);
                        return Mono.empty();
                    }
                    return Mono.error(ex);
                });
    }
}
