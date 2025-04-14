// src/main/java/com/banenor/config/DefaultUserInitializer.java
package com.banenor.config;

import com.banenor.model.User;
import com.banenor.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Configuration
public class DefaultUserInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${default.admin.username:admin}")
    private String defaultUsername;

    @Value("${default.admin.password:admin}")
    private String defaultPassword;

    @Value("${default.admin.email:admin@example.com}")
    private String defaultEmail;

    public DefaultUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initDefaultUser() {
        try {
            // Block until the default admin user is retrieved or created.
            User defaultUser = userRepository.findByUsername(defaultUsername)
                    .switchIfEmpty(Mono.defer(() -> {
                        User user = User.builder()
                                .username(defaultUsername)
                                .email(defaultEmail)
                                .password(passwordEncoder.encode(defaultPassword))
                                .role("ADMIN")
                                .enabled(true)
                                .locked(false) // Ensure account is not locked
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                        return userRepository.save(user);
                    }))
                    .block();

            if (defaultUser != null) {
                log.info("Default admin user is present with username: {}", defaultUser.getUsername());
            } else {
                log.error("Failed to initialize default admin user.");
            }
        } catch (Exception e) {
            log.error("Error during default admin user initialization", e);
        }
    }
}
