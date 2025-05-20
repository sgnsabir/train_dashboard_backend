package com.banenor.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.banenor.dto.AxlesDataDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    /** Reactive connection factory (primary). */
    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        var cfg = new RedisStandaloneConfiguration(redisHost, redisPort);
        var factory = new LettuceConnectionFactory(cfg);
        factory.afterPropertiesSet();
        return factory;
    }

    /** Blocking connection factory for Spring’s CacheManager. */
    @Bean
    @Qualifier("blockingRedisConnectionFactory")
    public RedisConnectionFactory blockingRedisConnectionFactory() {
        var cfg = new RedisStandaloneConfiguration(redisHost, redisPort);
        var factory = new LettuceConnectionFactory(cfg);
        factory.afterPropertiesSet();
        return factory;
    }

    /** General-purpose ReactiveRedisTemplate<String, Object>. */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory
    ) {
        var json = RedisSerializer.json();
        var ctx = RedisSerializationContext.<String, Object>newSerializationContext()
                .key(RedisSerializer.string())
                .value(json)
                .hashKey(RedisSerializer.string())
                .hashValue(json)
                .build();
        return new ReactiveRedisTemplate<>(factory, ctx);
    }

    /** Simple ReactiveRedisTemplate<String, String>. */
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory factory
    ) {
        var str = RedisSerializer.string();
        var ctx = RedisSerializationContext.<String, String>newSerializationContext()
                .key(str)
                .value(str)
                .hashKey(str)
                .hashValue(str)
                .build();
        return new ReactiveRedisTemplate<>(factory, ctx);
    }

    /**
     * Dedicated ReactiveRedisTemplate<String,AxlesDataDTO> so that
     * AxlesDataServiceImpl can autowire it directly.
     */
    @Bean
    public ReactiveRedisTemplate<String, AxlesDataDTO> axlesDataDtoRedisTemplate(
            ReactiveRedisConnectionFactory factory
    ) {
        // configure ObjectMapper for date handling, modules, etc.
        ObjectMapper mapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Jackson2JsonRedisSerializer with ObjectMapper + target class
        Jackson2JsonRedisSerializer<AxlesDataDTO> valueSer =
                new Jackson2JsonRedisSerializer<>(mapper, AxlesDataDTO.class);

        RedisSerializer<String> keySer = RedisSerializer.string();
        var ctx = RedisSerializationContext.<String, AxlesDataDTO>newSerializationContext(keySer)
                .value(valueSer)
                .hashKey(keySer)
                .hashValue(valueSer)
                .build();

        return new ReactiveRedisTemplate<>(factory, ctx);
    }

    /** Primary synchronous CacheManager for @Cacheable, etc. */
    @Bean
    @Primary
    public CacheManager cacheManager(
            @Qualifier("blockingRedisConnectionFactory") RedisConnectionFactory blockingFactory
    ) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> overrides = new HashMap<>();
        overrides.put("jwtBlacklist",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1)));
        overrides.put("averages",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));
        overrides.put("riskScore",
                defaultConfig.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(
                        RedisCacheWriter.nonLockingRedisCacheWriter(blockingFactory)
                )
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(overrides)
                .build();
    }
}
