package com.banenor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class CorsConfig {

    // Read allowed origins from properties; default to "http://localhost:3000" if not set.
    @Value("#{'${app.core.allowed-origins:http://localhost:3000}'.split(',')}")
    private List<String> allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        log.info("Initializing dynamic CORS configuration for reactive endpoints.");

        CorsConfiguration corsConfig = new CorsConfiguration();

        // Process and normalize the allowed origins from the properties.
        List<String> originPatterns = new ArrayList<>();
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            log.debug("Configured allowed origins: {}", allowedOrigins);
            originPatterns = allowedOrigins.stream()
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .map(this::normalizeOrigin)
                    .collect(Collectors.toList());
        } else {
            log.warn("No allowed origins specified. Defaulting to http://localhost:3000");
            originPatterns.add("http://localhost:3000");
        }
        log.debug("Normalized allowed origin patterns: {}", originPatterns);

        // Set the allowed origins (using patterns) and allow credentials.
        corsConfig.setAllowedOriginPatterns(originPatterns);
        corsConfig.setAllowCredentials(true);

        // Define allowed headers.
        List<String> allowedHeaders = List.of(
                "Authorization", "Cache-Control", "Content-Type", "Origin", "Accept", "X-Requested-With",
                "sec-ch-ua", "sec-ch-ua-mobile", "sec-ch-ua-platform"
        );
        corsConfig.setAllowedHeaders(allowedHeaders);
        log.debug("Allowed headers: {}", allowedHeaders);

        // Allow common HTTP methods.
        List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");
        corsConfig.setAllowedMethods(allowedMethods);
        log.debug("Allowed methods: {}", allowedMethods);

        // Expose the Authorization header.
        List<String> exposedHeaders = List.of("Authorization");
        corsConfig.setExposedHeaders(exposedHeaders);
        log.debug("Exposed headers: {}", exposedHeaders);

        // Set preflight cache duration (in seconds).
        corsConfig.setMaxAge(3600L);
        log.debug("Max age for preflight response set to 3600 seconds");

        // Apply the CORS configuration to all endpoints.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        log.info("CORS configuration applied with allowed origin patterns: {}", originPatterns);
        return new CorsWebFilter(source);
    }

    /**
     * Normalizes an origin string by removing any trailing slash.
     *
     * @param origin the origin string to normalize.
     * @return the normalized origin.
     */
    private String normalizeOrigin(String origin) {
        if (origin.endsWith("/")) {
            String normalized = origin.substring(0, origin.length() - 1);
            log.debug("Normalized origin from '{}' to '{}'", origin, normalized);
            return normalized;
        }
        return origin;
    }
}
