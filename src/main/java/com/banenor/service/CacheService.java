package com.banenor.service;

import reactor.core.publisher.Mono;

public interface CacheService {
    Mono<Void> cacheAverage(String key, Double value);
    Mono<Double> getCachedAverage(String key);
}
