package com.banenor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * Simple test security configuration that permits all exchanges,
 * avoiding 404 or 401/403 in the test environment.
 */
@Configuration
@EnableReactiveMethodSecurity
public class TestSecurityConfig {

    /**
     * Create a SecurityWebFilterChain that disables CSRF
     * and permits all requests.
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain() {
        ServerHttpSecurity http = ServerHttpSecurity.http();
        http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
        return http.build();
    }

@Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        // Provide a dummy authentication manager for test purposes.
        return authentication ->
                Mono.just(new UsernamePasswordAuthenticationToken("dummyUser", "dummyPassword", Collections.emptyList()));
    }
}
