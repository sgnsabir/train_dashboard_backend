package com.banenor.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Dynamically configurable CORS settings for WebFlux backend.
 */
@Configuration
@ConfigurationProperties(prefix = "cors")
@RefreshScope
@Data
@Slf4j
public class CorsConfig {

    /**
     * List of allowed origins (without trailing slashes).
     * Example: http://localhost:3000, https://app.example.com
     */
    private List<String> allowedOrigins = List.of("http://localhost:3000");

    /**
     * HTTP methods permitted for cross-origin requests.
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    /**
     * Headers that are allowed in requests.
     */
    private List<String> allowedHeaders = List.of(
            "Authorization", "Content-Type", "Origin", "Accept", "X-Requested-With",
            "Cache-Control", "sec-ch-ua", "sec-ch-ua-mobile", "sec-ch-ua-platform"
    );

    /**
     * Headers that are exposed in the browser.
     */
    private List<String> exposedHeaders = List.of("Authorization");

    /**
     * Cache duration (in seconds) for preflight responses.
     */
    private Long maxAge = 3600L;

    @Bean
    public CorsWebFilter corsWebFilter() {
        // Normalize allowed origins
        List<String> normalizedOrigins = allowedOrigins.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(this::normalize)
                .distinct()
                .collect(Collectors.toList());

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(normalizedOrigins); // supports wildcard subdomains
        config.setAllowedMethods(allowedMethods);
        config.setAllowedHeaders(allowedHeaders);
        config.setExposedHeaders(exposedHeaders);
        config.setAllowCredentials(true);
        config.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        log.info("✅ CORS Config initialized");
        log.info("  → Allowed Origins: {}", normalizedOrigins);
        log.info("  → Allowed Methods: {}", allowedMethods);
        log.info("  → Allowed Headers: {}", allowedHeaders);
        log.info("  → Exposed Headers: {}", exposedHeaders);
        log.info("  → Max Age (s): {}", maxAge);

        return new CorsWebFilter(source);
    }

    /**
     * Removes any trailing slash from the origin.
     */
    private String normalize(String origin) {
        return origin != null && origin.endsWith("/")
                ? origin.substring(0, origin.length() - 1)
                : origin;
    }
}
