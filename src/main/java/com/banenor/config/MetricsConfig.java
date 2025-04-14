package com.banenor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class MetricsConfig {

    /**
     * Customizes the MeterRegistry by adding common tags.
     * The auto-configured MeterRegistry (provided by Spring Boot Actuator)
     * will pick up these customizations.
     *
     * @param applicationName the application name (defaults to 'predictive-maintenance-backend')
     * @param environment     the environment (defaults to 'production')
     * @return a MeterRegistryCustomizer to apply common tags.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${spring.application.name:predictive-maintenance-backend}") String applicationName,
            @Value("${management.metrics.tags.environment:production}") String environment) {
        return registry -> registry.config().commonTags("application", applicationName, "environment", environment);
    }
}
