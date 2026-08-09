package com.steerlog.support;

import com.steerlog.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

public final class SecurityTestSupport {

    public static final Long TEST_USER_ID = 1L;
    public static final String TEST_EMAIL = "test@example.com";

    private SecurityTestSupport() {
    }

    public static RequestPostProcessor authenticatedUser() {
        return authentication(testAuthentication());
    }

    public static Authentication testAuthentication() {
        AuthenticatedUser user = new AuthenticatedUser(TEST_USER_ID, TEST_EMAIL);
        return new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    }
}
