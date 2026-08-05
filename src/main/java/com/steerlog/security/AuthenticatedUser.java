package com.steerlog.security;

import java.util.Objects;

public class AuthenticatedUser {

    private final Long userId;
    private final String email;

    public AuthenticatedUser(Long userId, String email) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.email = Objects.requireNonNull(email, "email");
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }
}
