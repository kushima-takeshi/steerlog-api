package com.steerlog.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("change-me-to-a-long-random-secret-key-32bytes");
        properties.setExpirationMs(86400000L);
        jwtService = new JwtService(properties);
    }

    @Test
    void createToken_andParseToken_shouldRoundTripUser() {
        String token = jwtService.createToken(42L, "user@example.com");

        AuthenticatedUser user = jwtService.parseToken(token);

        assertThat(user.getUserId()).isEqualTo(42L);
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(86400L);
    }

    @Test
    void parseToken_shouldThrowWhenTokenIsTampered() {
        String token = jwtService.createToken(42L, "user@example.com");
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOf(InvalidJwtException.class)
                .hasMessage("Invalid JWT");
    }

    @Test
    void parseToken_shouldThrowWhenTokenIsMalformed() {
        assertThatThrownBy(() -> jwtService.parseToken("not-a-jwt"))
                .isInstanceOf(InvalidJwtException.class)
                .hasMessage("Invalid JWT");
    }

    @Test
    void parseToken_shouldThrowWhenTokenIsExpired() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("change-me-to-a-long-random-secret-key-32bytes");
        properties.setExpirationMs(-1000L);
        JwtService expiredJwtService = new JwtService(properties);

        String token = expiredJwtService.createToken(42L, "user@example.com");

        assertThatThrownBy(() -> expiredJwtService.parseToken(token))
                .isInstanceOf(InvalidJwtException.class)
                .hasMessage("Invalid JWT");
    }
}
