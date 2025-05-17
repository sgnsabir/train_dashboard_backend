package com.banenor.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

@Slf4j
@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;  // same Base64 value as in JwtTokenUtil

    @Value("${security.password.encoder.strength:10}")
    private int bcryptStrength;

    private static final String[] PUBLIC_POST = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/reset-password"
    };

    private static final String[] PUBLIC_GET = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**",
            "/ws/**",
            "/sse/**"
    };

    /** Public endpoints chain */
    @Bean
    @Order(1)
    public SecurityWebFilterChain publicSecurityChain(ServerHttpSecurity http) {
        ServerWebExchangeMatcher publicMatcher = ServerWebExchangeMatchers.matchers(
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.OPTIONS, "/**"),
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, PUBLIC_POST),
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, PUBLIC_GET)
        );

        http
                .securityMatcher(publicMatcher)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> { /* global CORS filter if configured */ })
                .authorizeExchange(ex -> ex.anyExchange().permitAll());

        return http.build();
    }

    /** Protected endpoints chain */
    @Bean
    @Order(2)
    public SecurityWebFilterChain protectedSecurityChain(ServerHttpSecurity http) {
        Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtConverter = buildJwtConverter();

        http
                .securityMatcher(ServerWebExchangeMatchers.anyExchange())
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> { /* global CORS filter if configured */ })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, err) -> {
                            log.warn("Unauthorized: {}", err.getMessage());
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, err) -> {
                            log.warn("Forbidden: {}", err.getMessage());
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        })
                )
                .authorizeExchange(ex -> ex.anyExchange().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtConverter)
                        )
                );

        return http.build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> buildJwtConverter() {
        var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        var authConverter = new JwtAuthenticationConverter();
        authConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return new ReactiveJwtAuthenticationConverterAdapter(authConverter);
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager(
            ReactiveUserDetailsService uds,
            PasswordEncoder encoder
    ) {
        var mgr = new UserDetailsRepositoryReactiveAuthenticationManager(uds);
        mgr.setPasswordEncoder(encoder);
        return mgr;
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // decode the **same** Base64 secret
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return NimbusReactiveJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
    }
}
