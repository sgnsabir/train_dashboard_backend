package com.banenor.scheduler;

import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.service.CacheService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationScheduler {

    private final HaugfjellMP1AxlesRepository mp1AxlesRepository;
    private final HaugfjellMP3AxlesRepository mp3AxlesRepository;
    private final CacheService cacheService;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 60000)
    public void aggregateSensorData() {
        Timer.Sample sample = Timer.start(meterRegistry);

        // Aggregate average speed from both repositories.
        Mono<Double> avgSpeedMono = aggregateSpeedMetric();

        avgSpeedMono.doOnNext(avg -> {
                    // Cache the average speed under the key "avgSpeed"
                    cacheService.cacheAverage("avgSpeed", avg).subscribe();
                    meterRegistry.counter("aggregation.avgSpeed.executions").increment();
                    log.info("Aggregated avgSpeed: {}", avg);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> {
                    meterRegistry.counter("aggregation.errors").increment();
                    log.error("Error aggregating sensor data", e);
                })
                .doFinally(signal -> sample.stop(
                        Timer.builder("aggregation.execution.latency")
                                .description("Latency of sensor data aggregation process")
                                .tags("component", "aggregationScheduler")
                                .register(meterRegistry)
                ))
                .subscribe();
    }

    /**
     * Aggregates the global average speed from both MP1 and MP3 repositories.
     * Assumes that both repositories implement a method findGlobalAverageSpeed() returning Mono<Double>.
     *
     * @return a Mono emitting the average of the two global speeds.
     */
    private Mono<Double> aggregateSpeedMetric() {
        Mono<Double> speed1 = mp1AxlesRepository.findGlobalAverageSpeed().defaultIfEmpty(0.0);
        Mono<Double> speed2 = mp3AxlesRepository.findGlobalAverageSpeed().defaultIfEmpty(0.0);
        return Mono.zip(speed1, speed2, (s1, s2) -> (s1 + s2) / 2.0);
    }
}
