package com.steerlog.service;

import com.steerlog.dto.request.LoginRequest;
import com.steerlog.dto.request.RegisterRequest;
import com.steerlog.dto.response.AuthTokenResponse;
import com.steerlog.dto.response.AuthUserResponse;
import com.steerlog.entity.User;
import com.steerlog.exception.EmailAlreadyRegisteredException;
import com.steerlog.exception.InvalidCredentialsException;
import com.steerlog.repository.UserRepository;
import com.steerlog.security.AuthenticatedUser;
import com.steerlog.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("  Test@Example.COM ");
        request.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(10L);
            return user;
        });
        when(jwtService.createToken(10L, "test@example.com")).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        AuthTokenResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86400L);
        assertThat(response.getUser().getUserId()).isEqualTo(10L);
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void register_shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email already registered");

        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).createToken(any(), anyString());
    }

    @Test
    void register_shouldThrowWhenSaveHitsUniqueConstraint() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email already registered");
    }

    @Test
    void login_shouldReturnTokenWhenCredentialsMatch() {
        LoginRequest request = new LoginRequest();
        request.setEmail("  Test@Example.COM ");
        request.setPassword("password123");

        User user = buildUser(10L, "test@example.com", "hashed-password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.createToken(10L, "test@example.com")).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        AuthTokenResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86400L);
        assertThat(response.getUser().getUserId()).isEqualTo(10L);
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void login_shouldThrowWhenEmailNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).createToken(any(), anyString());
    }

    @Test
    void login_shouldThrowWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong-password");

        User user = buildUser(10L, "test@example.com", "hashed-password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(jwtService, never()).createToken(any(), anyString());
    }

    @Test
    void me_shouldReturnAuthenticatedUser() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(10L, "test@example.com");

        AuthUserResponse response = authService.me(authenticatedUser);

        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    private User buildUser(Long userId, String email, String passwordHash) {
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }
}
