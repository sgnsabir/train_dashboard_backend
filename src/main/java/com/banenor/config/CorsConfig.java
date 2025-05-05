package com.banenor.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "cors")
@Slf4j
public class CorsConfig implements WebFluxConfigurer {

    /**
     * Origins allowed for cross-origin requests.
     * Examples: http://localhost:3000, https://app.example.com
     */
    private List<String> allowedOrigins = List.of("http://localhost:3000");

    /**
     * HTTP methods permitted for cross-origin.
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    /**
     * Request headers allowed.
     */
    private List<String> allowedHeaders = List.of(
            "Authorization", "Content-Type", "Origin", "Accept", "X-Requested-With",
            "Cache-Control", "sec-ch-ua", "sec-ch-ua-mobile", "sec-ch-ua-platform"
    );

    /**
     * Response headers exposed to the browser.
     */
    private List<String> exposedHeaders = List.of("Authorization");

    /**
     * How long (in seconds) preflight responses are cached by the browser.
     */
    private Long maxAge = 3600L;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var origins = allowedOrigins.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(this::normalize)
                .distinct()
                .collect(Collectors.toList());

        registry.addMapping("/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods(allowedMethods.toArray(String[]::new))
                .allowedHeaders(allowedHeaders.toArray(String[]::new))
                .exposedHeaders(exposedHeaders.toArray(String[]::new))
                .allowCredentials(true)
                .maxAge(maxAge);

        log.info("CORS configuration initialized:");
        log.info("  allowedOrigins = {}", origins);
        log.info("  allowedMethods = {}", allowedMethods);
        log.info("  allowedHeaders = {}", allowedHeaders);
        log.info("  exposedHeaders = {}", exposedHeaders);
        log.info("  maxAge         = {}s", maxAge);
    }

    // strip trailing slash if present
    private String normalize(String origin) {
        if (origin.endsWith("/")) {
            return origin.substring(0, origin.length() - 1);
        }
        return origin;
    }
}
