package com.banenor.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    /**
     * Bean for reactive operations.
     * Marked as @Primary so that it’s selected by default when injecting a ReactiveRedisConnectionFactory.
     */
    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * Bean for blocking operations (e.g., cache management).
     * Qualified as "blockingRedisConnectionFactory" to avoid mix-up with reactive beans.
     */
    @Bean
    @Qualifier("blockingRedisConnectionFactory")
    public RedisConnectionFactory blockingRedisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * Defines a RedisTemplate bean for blocking operations.
     * This bean is required by components expecting a bean named "redisTemplate".
     */
    @Bean(name = "redisTemplate")
    public RedisTemplate<Object, Object> redisTemplate(@Qualifier("blockingRedisConnectionFactory") RedisConnectionFactory blockingRedisConnectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(blockingRedisConnectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Builds the RedisCacheManager using a blocking connection factory.
     * The cache writer is created via non-locking RedisCacheWriter using the blocking connection.
     */
    @Bean
    public RedisCacheManager reactiveCacheManager(@Qualifier("blockingRedisConnectionFactory") RedisConnectionFactory blockingRedisConnectionFactory) {
        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(blockingRedisConnectionFactory);

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("averages", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("riskScore", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
