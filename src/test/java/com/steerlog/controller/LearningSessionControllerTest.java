package com.steerlog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steerlog.dto.request.StartLearningSessionRequest;
import com.steerlog.dto.response.LearningSessionNextActionResponse;
import com.steerlog.dto.response.LearningSessionResponse;
import com.steerlog.dto.response.LearningSessionStepResponse;
import com.steerlog.entity.LearningSessionStatus;
import com.steerlog.entity.LearningSessionType;
import com.steerlog.exception.GlobalExceptionHandler;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.service.LearningSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LearningSessionController.class)
@Import(GlobalExceptionHandler.class)
class LearningSessionControllerTest {

    private static final Long TEMP_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LearningSessionService learningSessionService;

    @Test
    void startSession_shouldReturn201() throws Exception {
        Long resourceId = 10L;
        Instant startedAt = Instant.parse("2026-06-08T10:00:00Z");

        StartLearningSessionRequest request = new StartLearningSessionRequest();
        request.setSessionType(LearningSessionType.IMMEDIATE_REFLECTION);

        LearningSessionStepResponse step = new LearningSessionStepResponse();
        step.setCurrentStep(1);
        step.setTotalSteps(3);
        step.setIsFinalStep(false);

        LearningSessionNextActionResponse nextAction = new LearningSessionNextActionResponse();
        nextAction.setType("SUBMIT_RESPONSE");

        LearningSessionResponse response = new LearningSessionResponse();
        response.setLearningSessionId(700L);
        response.setResourceId(resourceId);
        response.setSessionType(LearningSessionType.IMMEDIATE_REFLECTION);
        response.setStatus(LearningSessionStatus.IN_PROGRESS);
        response.setAiPrompt("このResourceで学んだ内容を、自分の言葉で説明してください。");
        response.setStep(step);
        response.setNextAction(nextAction);
        response.setStartedAt(startedAt);

        when(learningSessionService.startSession(
                eq(TEMP_USER_ID), eq(resourceId), any(StartLearningSessionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/resources/{resourceId}/learning-sessions", resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.learningSessionId").value(700))
                .andExpect(jsonPath("$.resourceId").value(10))
                .andExpect(jsonPath("$.sessionType").value("IMMEDIATE_REFLECTION"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.aiPrompt")
                        .value("このResourceで学んだ内容を、自分の言葉で説明してください。"))
                .andExpect(jsonPath("$.step.currentStep").value(1))
                .andExpect(jsonPath("$.step.totalSteps").value(3))
                .andExpect(jsonPath("$.step.isFinalStep").value(false))
                .andExpect(jsonPath("$.nextAction.type").value("SUBMIT_RESPONSE"))
                .andExpect(jsonPath("$.startedAt").value("2026-06-08T10:00:00Z"));

        verify(learningSessionService).startSession(
                eq(TEMP_USER_ID), eq(resourceId), any(StartLearningSessionRequest.class));
    }

    @Test
    void startSession_shouldReturn400WhenSessionTypeMissing() throws Exception {
        Long resourceId = 10L;

        mockMvc.perform(post("/resources/{resourceId}/learning-sessions", resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startSession_shouldReturn404WhenResourceNotFound() throws Exception {
        Long resourceId = 10L;

        StartLearningSessionRequest request = new StartLearningSessionRequest();
        request.setSessionType(LearningSessionType.IMMEDIATE_REFLECTION);

        when(learningSessionService.startSession(
                eq(TEMP_USER_ID), eq(resourceId), any(StartLearningSessionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Resource not found"));

        mockMvc.perform(post("/resources/{resourceId}/learning-sessions", resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));

        verify(learningSessionService).startSession(
                eq(TEMP_USER_ID), eq(resourceId), any(StartLearningSessionRequest.class));
    }
}
