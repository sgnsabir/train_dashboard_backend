package com.banenor.service;

import com.banenor.dto.*;
import com.banenor.exception.InvalidCredentialsException;
import com.banenor.exception.UserAlreadyExistsException;
import com.banenor.exception.UserNotFoundException;
import com.banenor.model.User;
import com.banenor.repository.UserRepository;
import com.banenor.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private ReactiveAuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void testRegister_Success() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password");

        when(userRepository.findByUsername("testuser")).thenReturn(Mono.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());
        when(passwordEncoder.encode("password")).thenReturn("hashedPassword");

        User savedUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword")
                .role("USER")
                .build();
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));

        Mono<User> result = authService.register(request);
        StepVerifier.create(result)
                .assertNext(user -> {
                    assertNotNull(user, "Registered user should not be null");
                    assertEquals("testuser", user.getUsername(), "Username should match");
                })
                .verifyComplete();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegister_UserAlreadyExists() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("existingUser");
        request.setEmail("existing@example.com");
        request.setPassword("password");

        when(userRepository.findByUsername("existingUser")).thenReturn(Mono.just(new User()));
        StepVerifier.create(authService.register(request))
                .expectError(UserAlreadyExistsException.class)
                .verify();
    }

    @Test
    void testLogin_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password");

        // Create a mock Authentication object to simulate successful authentication
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(Mono.just(mockAuth));

        // Capture the authentication token passed to authenticationManager.
        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("testuser")
                .password("hashedPassword")
                .authorities("ROLE_USER")
                .build();
        when(userDetailsService.findByUsername("testuser")).thenReturn(Mono.just(userDetails));
        when(jwtUtil.generateToken("testuser", userDetails.getAuthorities().toString()))
                .thenReturn("jwtToken");

        Mono<LoginResponse> response = authService.login(request);
        StepVerifier.create(response)
                .assertNext(jwtResponse -> {
                    assertNotNull(jwtResponse, "JWT response should not be null");
                    assertEquals("jwtToken", jwtResponse.getToken(), "JWT token should match expected value");
                })
                .verifyComplete();

        verify(authenticationManager).authenticate(tokenCaptor.capture());
        UsernamePasswordAuthenticationToken capturedToken = tokenCaptor.getValue();
        assertEquals("testuser", capturedToken.getPrincipal(), "Principal should be 'testuser'");
        assertEquals("password", capturedToken.getCredentials(), "Credentials should be 'password'");
    }

    @Test
    void testLogin_InvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(Mono.error(new BadCredentialsException("Bad credentials")));
        StepVerifier.create(authService.login(request))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void testLogout() {
        Mono<Void> result = authService.logout("sampleToken");
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void testResetPassword_Success() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("test@example.com");
        request.setNewPassword("newPassword");

        User existingUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .password("oldPassword")
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Mono.just(existingUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(existingUser));

        Mono<Void> result = authService.resetPassword(request);
        StepVerifier.create(result)
                .verifyComplete();

        verify(userRepository).save(argThat(user -> "hashedNewPassword".equals(user.getPassword())));
    }

    @Test
    void testResetPassword_UserNotFound() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("nonexistent@example.com");
        request.setNewPassword("newPassword");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Mono.empty());
        StepVerifier.create(authService.resetPassword(request))
                .expectError(UserNotFoundException.class)
                .verify();
    }
}
