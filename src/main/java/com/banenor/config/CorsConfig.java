package com.banenor.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "cors")
@Slf4j
@Data
public class CorsConfig {

    /**
     * List of allowed origins; configured via:
     *   cors.allowed-origins=http://localhost:3000,https://mydomain.com
     */
    private List<String> allowedOrigins = List.of("http://localhost:3000");

    @Bean
    public CorsWebFilter corsWebFilter() {
        log.info("Initializing CORS with allowedOrigins={}", allowedOrigins);
        CorsConfiguration cors = new CorsConfiguration();

        List<String> origins = allowedOrigins.stream()
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .map(this::normalize)
                .collect(Collectors.toList());

        cors.setAllowedOriginPatterns(origins);
        cors.setAllowCredentials(true);
        cors.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cors.setAllowedHeaders(List.of(
                "Authorization","Cache-Control","Content-Type","Origin","Accept","X-Requested-With",
                "sec-ch-ua","sec-ch-ua-mobile","sec-ch-ua-platform"
        ));
        cors.setExposedHeaders(List.of("Authorization"));
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cors);

        log.debug("Applied CORS config for patterns: {}", origins);
        return new CorsWebFilter(src);
    }

    private String normalize(String origin) {
        return origin.endsWith("/") ? origin.substring(0, origin.length()-1) : origin;
    }
}
