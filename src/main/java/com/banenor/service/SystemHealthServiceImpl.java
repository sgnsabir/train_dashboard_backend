package com.banenor.service;

import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class SystemHealthServiceImpl implements SystemHealthService {

    private static final Logger logger = LoggerFactory.getLogger(SystemHealthServiceImpl.class);

    private final ConnectionFactory connectionFactory;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ReactiveRedisConnectionFactory redisConnectionFactory;

    public SystemHealthServiceImpl(ConnectionFactory connectionFactory,
                                   KafkaTemplate<String, String> kafkaTemplate,
                                   ReactiveRedisConnectionFactory redisConnectionFactory) {
        this.connectionFactory = connectionFactory;
        this.kafkaTemplate = kafkaTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Mono<String> getSystemStatus() {
        // Non-blocking database health check using R2DBC
        Mono<Boolean> dbHealthyMono = DatabaseClient.create(connectionFactory)
                .sql("SELECT 1")
                .fetch()
                .one()
                .map(result -> true)
                .onErrorResume(e -> {
                    logger.error("Database health check failed", e);
                    return Mono.just(false);
                });

        // Wrap Kafka health check (blocking) in a boundedElastic scheduler
        Mono<Boolean> kafkaHealthyMono = Mono.fromCallable(() -> {
            try {
                kafkaTemplate.getProducerFactory().createProducer().close();
                return true;
            } catch (Exception e) {
                logger.error("Kafka health check failed", e);
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());

        // Non-blocking Redis health check using reactive API
        Mono<Boolean> redisHealthyMono = redisConnectionFactory.getReactiveConnection()
                .ping()
                .map("PONG"::equalsIgnoreCase)
                .onErrorResume(e -> {
                    logger.error("Redis health check failed", e);
                    return Mono.just(false);
                });

        // Combine health checks reactively
        return Mono.zip(dbHealthyMono, kafkaHealthyMono, redisHealthyMono)
                .map(tuple -> {
                    boolean dbHealthy = tuple.getT1();
                    boolean kafkaHealthy = tuple.getT2();
                    boolean redisHealthy = tuple.getT3();

                    if (dbHealthy && kafkaHealthy && redisHealthy) {
                        logger.info("System health check: All components operational.");
                        return "Operational";
                    } else {
                        StringBuilder status = new StringBuilder("Degraded: ");
                        if (!dbHealthy) {
                            status.append("Database Down; ");
                        }
                        if (!kafkaHealthy) {
                            status.append("Kafka Down; ");
                        }
                        if (!redisHealthy) {
                            status.append("Redis Down; ");
                        }
                        String finalStatus = status.toString().trim();
                        logger.warn("System health check: {}", finalStatus);
                        return finalStatus;
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
