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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }

        Instant now = Instant.now();
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }

        return toAuthTokenResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return toAuthTokenResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me(AuthenticatedUser authenticatedUser) {
        return new AuthUserResponse(authenticatedUser.getUserId(), authenticatedUser.getEmail());
    }

    private AuthTokenResponse toAuthTokenResponse(User user) {
        String accessToken = jwtService.createToken(user.getUserId(), user.getEmail());
        AuthUserResponse userResponse = new AuthUserResponse(user.getUserId(), user.getEmail());
        return new AuthTokenResponse(
                accessToken, TOKEN_TYPE, jwtService.getExpirationSeconds(), userResponse);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
