package com.banenor.security;

import com.banenor.service.CustomUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

@Slf4j
public class CustomReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public CustomReactiveAuthenticationManager(CustomUserDetailsService userDetailsService,
                                               PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
            log.error("Unsupported authentication token type: {}", authentication.getClass());
            return Mono.error(new IllegalArgumentException("Unsupported authentication token type: " + authentication.getClass()));
        }
        String username = authentication.getPrincipal().toString();
        String password = authentication.getCredentials().toString();
        log.debug("Authenticating user: {}", username);
        return userDetailsService.findByUsername(username)
                .flatMap(userDetails -> {
                    if (passwordEncoder.matches(password, userDetails.getPassword())) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        log.debug("Authentication successful for user: {}", username);
                        return Mono.just((Authentication) authToken);
                    } else {
                        log.error("Authentication failed for user: {}. Invalid credentials.", username);
                        return Mono.error(new BadCredentialsException("Invalid credentials"));
                    }
                })
                .switchIfEmpty(Mono.error(new BadCredentialsException("User not found")));
    }
}
