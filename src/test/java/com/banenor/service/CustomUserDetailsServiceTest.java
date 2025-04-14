package com.banenor.service;

import com.banenor.model.User;
import com.banenor.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.TestPropertySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(locations = "classpath:test.properties")
public class CustomUserDetailsServiceTest {

    private static final String TEST_USER_EMAIL = "testuser@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String NULL_ROLE_EMAIL = "nullroleuser@example.com";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Load user by username successfully with USER role")
    void testFindByUsername_Success_UserRole() {
        User user = User.builder()
                .userId(1L)
                .username("testuser")
                .password("hashedPassword")
                .role("USER")
                .email(TEST_USER_EMAIL)
                .build();
        when(userRepository.findByUsername("testuser")).thenReturn(Mono.just(user));

        Mono<UserDetails> userDetailsMono = customUserDetailsService.findByUsername("testuser");

        StepVerifier.create(userDetailsMono)
                .assertNext(userDetails -> {
                    assertThat(userDetails).isNotNull();
                    assertThat(userDetails.getUsername()).isEqualTo("testuser");
                    assertThat(userDetails.getPassword()).isEqualTo("hashedPassword");
                    assertThat(userDetails.getAuthorities()).hasSize(1);
                    assertThat(userDetails.getAuthorities())
                            .extracting(GrantedAuthority::getAuthority)
                            .containsExactly("ROLE_USER");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Load user by username successfully with ADMIN role (case insensitive)")
    void testFindByUsername_Success_AdminRole() {
        User user = User.builder()
                .userId(2L)
                .username("adminUser")
                .password("adminHashed")
                .role("admin") // even if provided in lowercase, our implementation produces ROLE_ADMIN
                .email(ADMIN_EMAIL)
                .build();
        when(userRepository.findByUsername("adminUser")).thenReturn(Mono.just(user));

        Mono<UserDetails> userDetailsMono = customUserDetailsService.findByUsername("adminUser");

        StepVerifier.create(userDetailsMono)
                .assertNext(userDetails -> {
                    assertThat(userDetails).isNotNull();
                    assertThat(userDetails.getUsername()).isEqualTo("adminUser");
                    assertThat(userDetails.getPassword()).isEqualTo("adminHashed");
                    assertThat(userDetails.getAuthorities()).hasSize(1);
                    assertThat(userDetails.getAuthorities())
                            .extracting(GrantedAuthority::getAuthority)
                            .containsExactly("ROLE_ADMIN");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Throw UsernameNotFoundException for non-existing user")
    void testFindByUsername_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Mono.empty());
        StepVerifier.create(customUserDetailsService.findByUsername("nonexistent"))
                .expectError(UsernameNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("Throw exception when user role is null")
    void testFindByUsername_NullRole() {
        User user = User.builder()
                .userId(3L)
                .username("nullRoleUser")
                .password("pass")
                .role(null)
                .email(NULL_ROLE_EMAIL)
                .build();
        when(userRepository.findByUsername("nullRoleUser")).thenReturn(Mono.just(user));

        StepVerifier.create(customUserDetailsService.findByUsername("nullRoleUser"))
                .expectError(NullPointerException.class)
                .verify();
    }

    @Test
    @DisplayName("Throw UsernameNotFoundException when username is blank")
    void testFindByUsername_BlankUsername() {
        when(userRepository.findByUsername("")).thenReturn(Mono.empty());
        StepVerifier.create(customUserDetailsService.findByUsername(""))
                .expectError(UsernameNotFoundException.class)
                .verify();
    }
}
