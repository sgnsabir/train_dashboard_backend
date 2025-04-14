package com.banenor.filter;

import com.banenor.util.IPUtils;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public class RateLimitingFilter implements WebFilter {

    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    // Increased defaults to support realtime streaming dashboards.
    @Value("${rate.limit.requests-per-minute:12000}")
    private int maxRequestsPerMinute;

    @Value("${rate.limit.burst-size:2000}")
    private int burstSize;

    @Value("${rate.limit.enabled:true}")
    private boolean rateLimitingEnabled;

    @Value("${rate.limit.cleanup-threshold:10000}")
    private int cleanupThreshold;

    @Value("${rate.limit.cleanup-interval-ms:300000}")
    private long cleanupInterval;

    public RateLimitingFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        // Register metrics for active client IP count.
        meterRegistry.gauge("rate_limiting.active_clients", requestCounts, ConcurrentHashMap::size);
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        // Bypass rate limiting for OPTIONS (preflight) requests.
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            log.debug("Bypassing rate limiting for preflight OPTIONS request: {}", exchange.getRequest().getURI());
            return chain.filter(exchange);
        }

        if (!rateLimitingEnabled || shouldSkipRateLimit(exchange)) {
            return chain.filter(exchange);
        }

        return Mono.defer(() -> {
            try {
                // Extract client IP using IPUtils.
                String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
                String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
                String remoteAddress = exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown";
                String clientIP = IPUtils.extractClientIP(xForwardedFor, xRealIp, remoteAddress);

                log.debug("Processing request from client IP: {}", clientIP);

                requestCounts.putIfAbsent(clientIP, new AtomicInteger(0));
                int requests = requestCounts.get(clientIP).incrementAndGet();

                // Record request metrics using a masked IP.
                meterRegistry.counter("rate_limiting.requests", "client_ip", IPUtils.maskIP(clientIP)).increment();

                if (requests > maxRequestsPerMinute) {
                    // Allow bursts up to burstSize over the limit for a short duration.
                    if (requests <= maxRequestsPerMinute + burstSize) {
                        addRateLimitHeaders(exchange, requests);
                        log.debug("Client IP {} is in burst limit range: {} requests", IPUtils.maskIP(clientIP), requests);
                        return chain.filter(exchange)
                                .doOnError(e -> handleFilterError(e, clientIP));
                    }
                    log.warn("Rate limit exceeded for client IP: {} ({} requests)", IPUtils.maskIP(clientIP), requests);
                    return handleRateLimitExceeded(exchange, clientIP);
                }

                addRateLimitHeaders(exchange, requests);
                return chain.filter(exchange)
                        .doOnError(e -> handleFilterError(e, clientIP));

            } catch (Exception e) {
                log.error("Unexpected error in rate limiting filter: {}", e.getMessage(), e);
                meterRegistry.counter("rate_limiting.unexpected_errors").increment();
                return chain.filter(exchange);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private boolean shouldSkipRateLimit(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value().toLowerCase();
        // Skip rate limiting for health check endpoints.
        return path.startsWith("/actuator/health") || path.startsWith("/actuator/info");
    }

    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, String clientIP) {
        meterRegistry.counter("rate_limiting.exceeded", "client_ip", IPUtils.maskIP(clientIP)).increment();
        log.warn("Rate limit exceeded for IP: {}", IPUtils.maskIP(clientIP));

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(maxRequestsPerMinute));
        exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", "0");
        exchange.getResponse().getHeaders().set("Retry-After", "60");

        String responseBody = String.format(
                "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again in 60 seconds.\",\"status\":%d}",
                HttpStatus.TOO_MANY_REQUESTS.value()
        );
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void addRateLimitHeaders(ServerWebExchange exchange, int requests) {
        exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(maxRequestsPerMinute));
        exchange.getResponse().getHeaders().set("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, maxRequestsPerMinute - requests)));
    }

    private void handleFilterError(Throwable e, String clientIP) {
        log.error("Error processing request for IP {}: {}", IPUtils.maskIP(clientIP), e.getMessage());
        meterRegistry.counter("rate_limiting.errors", "client_ip", IPUtils.maskIP(clientIP),
                "error", e.getClass().getSimpleName()).increment();
    }

    @Scheduled(fixedRate = 60000)
    public void resetCounts() {
        try {
            int previousSize = requestCounts.size();
            requestCounts.clear();
            log.debug("Rate limit counters reset. Cleared {} client entries", previousSize);
            meterRegistry.counter("rate_limiting.resets").increment();
        } catch (Exception e) {
            log.error("Error during rate limit counter reset: {}", e.getMessage(), e);
            meterRegistry.counter("rate_limiting.reset_errors").increment();
        }
    }

    @Scheduled(fixedRateString = "#{${rate.limit.cleanup-interval-ms:300000}}")
    public void cleanup() {
        try {
            if (requestCounts.size() > cleanupThreshold) {
                log.warn("High number of rate limit entries ({}), performing cleanup", requestCounts.size());
                requestCounts.clear();
                meterRegistry.counter("rate_limiting.emergency_cleanups").increment();
            }
        } catch (Exception e) {
            log.error("Error during rate limit cleanup: {}", e.getMessage(), e);
            meterRegistry.counter("rate_limiting.cleanup_errors").increment();
        }
    }
}
