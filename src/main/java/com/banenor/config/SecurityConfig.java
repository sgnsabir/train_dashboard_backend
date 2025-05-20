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
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
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
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

@Slf4j
@Configuration
@EnableWebFluxSecurity                           // ← needed for WebFlux
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Value("${jwt.secret:AZmsBHO2Oz2ZFXctJprovHnw2qHaWpyLmunQjz35U6w=}")
    private String jwtSecret;

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

    /**
     * Public chain: OPTIONS + our auth POSTs + docs/actuator/WS/SSE GETs.
     */
    @Bean @Order(1)
    public SecurityWebFilterChain publicSecurityChain(ServerHttpSecurity http) {
        http
                .securityMatcher(ServerWebExchangeMatchers.matchers(
                        ServerWebExchangeMatchers.pathMatchers(HttpMethod.OPTIONS, "/**"),
                        ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, PUBLIC_POST),
                        ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, PUBLIC_GET)
                ))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> { /* rely on global CorsWebFilter if you have one */ })
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(ex -> ex.anyExchange().permitAll());

        return http.build();
    }

    /**
     * Protected chain: everything else requires a valid JWT.
     */
    @Bean @Order(2)
    public SecurityWebFilterChain protectedSecurityChain(ServerHttpSecurity http) {
        Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtConverter = jwtConverter();

        http
                .securityMatcher(ServerWebExchangeMatchers.anyExchange())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> { /* rely on global CorsWebFilter */ })
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, ex2) -> {
                            log.warn("Unauthorized: {}", ex2.getMessage());
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, ex3) -> {
                            log.warn("Forbidden: {}", ex3.getMessage());
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

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtConverter() {
        JwtGrantedAuthoritiesConverter authz = new JwtGrantedAuthoritiesConverter();
        authz.setAuthorityPrefix("ROLE_");
        authz.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(authz);

        return new ReactiveJwtAuthenticationConverterAdapter(conv);
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
        byte[] key = Decoders.BASE64.decode(jwtSecret);
        SecretKey sk = Keys.hmacShaKeyFor(key);
        return NimbusReactiveJwtDecoder.withSecretKey(sk).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
    }
}
