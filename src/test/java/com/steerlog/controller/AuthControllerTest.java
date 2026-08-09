package com.steerlog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steerlog.dto.request.LoginRequest;
import com.steerlog.dto.request.RegisterRequest;
import com.steerlog.dto.response.AuthTokenResponse;
import com.steerlog.dto.response.AuthUserResponse;
import com.steerlog.exception.EmailAlreadyRegisteredException;
import com.steerlog.exception.GlobalExceptionHandler;
import com.steerlog.exception.InvalidCredentialsException;
import com.steerlog.security.AuthenticatedUser;
import com.steerlog.service.AuthService;
import com.steerlog.support.AuthenticatedControllerTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.steerlog.support.SecurityTestSupport.TEST_EMAIL;
import static com.steerlog.support.SecurityTestSupport.TEST_USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest extends AuthenticatedControllerTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void register_shouldReturn201WithToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authService.register(any(RegisterRequest.class))).thenReturn(buildTokenResponse());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andExpect(jsonPath("$.user.userId").value(10))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void register_shouldReturn409WhenEmailAlreadyRegistered() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyRegisteredException("Email already registered"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void register_shouldReturn400WhenPasswordTooShort() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("short");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturn200WithToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class))).thenReturn(buildTokenResponse());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andExpect(jsonPath("$.user.userId").value(10))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void login_shouldReturn401WhenCredentialsInvalid() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong-password");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void me_shouldReturn200WithAuthenticatedUser() throws Exception {
        when(authService.me(any(AuthenticatedUser.class)))
                .thenReturn(new AuthUserResponse(TEST_USER_ID, TEST_EMAIL));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID.intValue()))
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));

        verify(authService).me(any(AuthenticatedUser.class));
    }

    @Test
    void me_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/auth/me").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    private AuthTokenResponse buildTokenResponse() {
        AuthUserResponse user = new AuthUserResponse(10L, "test@example.com");
        return new AuthTokenResponse("jwt-token", "Bearer", 86400L, user);
    }
}
