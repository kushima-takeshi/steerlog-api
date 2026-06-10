package com.steerlog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steerlog.dto.request.StartLearningSessionRequest;
import com.steerlog.dto.request.SubmitLearningSessionResponseRequest;
import com.steerlog.dto.response.CompleteLearningSessionResponse;
import com.steerlog.dto.response.DiscardLearningSessionResponse;
import com.steerlog.dto.response.LearningSessionResultDraftResponse;
import com.steerlog.dto.response.SubmitLearningSessionResponseResponse;
import com.steerlog.dto.response.LearningSessionNextActionResponse;
import com.steerlog.dto.response.LearningSessionResponse;
import com.steerlog.dto.response.LearningSessionStepResponse;
import com.steerlog.entity.LearningSessionStatus;
import com.steerlog.entity.LearningSessionType;
import com.steerlog.exception.GlobalExceptionHandler;
import com.steerlog.exception.LearningSessionCannotAcceptResponseException;
import com.steerlog.exception.LearningSessionCannotBeCompletedException;
import com.steerlog.exception.LearningSessionCannotBeDiscardedException;
import com.steerlog.exception.LearningSessionNotFoundException;
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
import java.util.List;

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

    @Test
    void discardSession_shouldReturn200() throws Exception {
        Long learningSessionId = 700L;
        Long resourceId = 10L;
        Instant updatedAt = Instant.parse("2026-06-08T12:00:00Z");

        DiscardLearningSessionResponse response = new DiscardLearningSessionResponse();
        response.setLearningSessionId(learningSessionId);
        response.setResourceId(resourceId);
        response.setSessionType(LearningSessionType.IMMEDIATE_REFLECTION);
        response.setStatus(LearningSessionStatus.DISCARDED);
        response.setUpdatedAt(updatedAt);

        when(learningSessionService.discardSession(TEMP_USER_ID, resourceId, learningSessionId))
                .thenReturn(response);

        mockMvc.perform(post(
                        "/resources/{resourceId}/learning-sessions/{learningSessionId}/discard",
                        resourceId, learningSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learningSessionId").value(700))
                .andExpect(jsonPath("$.resourceId").value(10))
                .andExpect(jsonPath("$.sessionType").value("IMMEDIATE_REFLECTION"))
                .andExpect(jsonPath("$.status").value("DISCARDED"))
                .andExpect(jsonPath("$.updatedAt").value("2026-06-08T12:00:00Z"));

        verify(learningSessionService).discardSession(TEMP_USER_ID, resourceId, learningSessionId);
    }

    @Test
    void discardSession_shouldReturn404WhenSessionNotFound() throws Exception {
        Long resourceId = 10L;
        Long learningSessionId = 700L;

        when(learningSessionService.discardSession(TEMP_USER_ID, resourceId, learningSessionId))
                .thenThrow(new LearningSessionNotFoundException("Learning session not found"));

        mockMvc.perform(post(
                        "/resources/{resourceId}/learning-sessions/{learningSessionId}/discard",
                        resourceId, learningSessionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Learning session not found"));

        verify(learningSessionService).discardSession(TEMP_USER_ID, resourceId, learningSessionId);
    }

    @Test
    void discardSession_shouldReturn400WhenSessionCannotBeDiscarded() throws Exception {
        Long resourceId = 10L;
        Long learningSessionId = 700L;

        when(learningSessionService.discardSession(TEMP_USER_ID, resourceId, learningSessionId))
                .thenThrow(new LearningSessionCannotBeDiscardedException("Learning session cannot be discarded"));

        mockMvc.perform(post(
                        "/resources/{resourceId}/learning-sessions/{learningSessionId}/discard",
                        resourceId, learningSessionId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LEARNING_SESSION_CANNOT_BE_DISCARDED"))
                .andExpect(jsonPath("$.message").value("Learning session cannot be discarded"));

        verify(learningSessionService).discardSession(TEMP_USER_ID, resourceId, learningSessionId);
    }

    @Test
    void submitResponse_shouldReturn200() throws Exception {
        Long resourceId = 10L;
        Long learningSessionId = 800L;
        Instant updatedAt = Instant.parse("2026-06-08T12:00:00Z");

        SubmitLearningSessionResponseRequest request = new SubmitLearningSessionResponseRequest();
        request.setResponseText("REST APIの基本を説明した");

        LearningSessionStepResponse step = new LearningSessionStepResponse();
        step.setCurrentStep(2);
        step.setTotalSteps(3);
        step.setIsFinalStep(false);

        LearningSessionNextActionResponse nextAction = new LearningSessionNextActionResponse();
        nextAction.setType("SUBMIT_RESPONSE");

        SubmitLearningSessionResponseResponse response = new SubmitLearningSessionResponseResponse();
        response.setLearningSessionId(learningSessionId);
        response.setResourceId(resourceId);
        response.setSessionType(LearningSessionType.IMMEDIATE_REFLECTION);
        response.setStatus(LearningSessionStatus.IN_PROGRESS);
        response.setAiPrompt("なぜそれが重要だと思ったか、具体例を交えて説明してください。");
        response.setStep(step);
        response.setNextAction(nextAction);
        response.setUpdatedAt(updatedAt);

        when(learningSessionService.submitResponse(
                eq(TEMP_USER_ID), eq(resourceId), eq(learningSessionId),
                any(SubmitLearningSessionResponseRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post(
                        "/resources/{resourceId}/learning-sessions/{learningSessionId}/responses",
                        resourceId, learningSessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learningSessionId").value(800))
                .andExpect(jsonPath("$.resourceId").value(10))
                .andExpect(jsonPath("$.sessionType").value("IMMEDIATE_REFLECTION"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.aiPrompt")
                        .value("なぜそれが重要だと思ったか、具体例を交えて説明してください。"))
                .andExpect(jsonPath("$.step.currentStep").value(2))
                .andExpect(jsonPath("$.step.totalSteps").value(3))
                .andExpect(jsonPath("$.step.isFinalStep").value(false))
                .andExpect(jsonPath("$.nextAction.type").value("SUBMIT_RESPONSE"))
                .andExpect(jsonPath("$.updatedAt").value("2026-06-08T12:00:00Z"));

        verify(learningSessionService).submitResponse(
                eq(TEMP_USER_ID), eq(resourceId), eq(learningSessionId),
                any(SubmitLearningSessionResponseRequest.class));
    }

    @Test
    void submitResponse_shouldReturn400WhenResponseTextEmpty() throws Exception {
        Long resourceId = 10L;
        Long learningSessionId = 800L;

        mockMvc.perform(post(
                        "/resources/{resourceId}/learning-sessions/{learningSessionId}/responses",
                        resourceId, learningSessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseText\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitResponse_shouldReturn404WhenSessionNotFound() throws Exception {
        Long resourceId = 10L;
        Long learningSessionId = 800L;

        SubmitLearningSessionResponseRequest request = new SubmitLearningSessionResponseRequest();
        request.setResponseText("回答本文");

        when(learningSessionService.submitResponse(
                eq(TEMP_USER_ID), eq(resourceId), eq(learningSessionId),
                any(SubmitLearningSessionResponseRequest.class)))
                .thenThrow(new LearningSessionNotFoundException("Learning session not found"));

        mockMvc.perform(post(
                        "/resources/{resourceId}/learning-sessions/{learningSessionId}/responses",
                        resourceId, learningSessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Learning session not found"));

        verify(learningSessionService).submitResponse(
                eq(TEMP_USER_ID), eq(resourceId), eq(learningSessionId),
                any(SubmitLearningSessionResponseRequest.class));
    }

    @Test
    void submitResponse_shouldReturn400WhenSessionCannotAcceptResponse() throws Exception {
        Long resourceId = 10L;
        Long learningSessionId = 800L;

        SubmitLearningSessionResponseRequest request = new SubmitLearningSessionResponseRequest();
        request.setResponseText("回答本文");

        when(learningSessionService.submitResponse(
                eq(TEMP_USER_ID), eq(resourceId), eq(learningSessionId),
                any(SubmitLearningSessionResponseRequest.class)))
                .thenThrow(new LearningSessionCannotAcceptResponseException(
                        "Learning session cannot accept response"));

        mockMvc.perform(post(
                        "/resources/{resourceId}/learning-sessions/{learningSessionId}/responses",
                        resourceId, learningSessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LEARNING_SESSION_CANNOT_ACCEPT_RESPONSE"))
                .andExpect(jsonPath("$.message").value("Learning session cannot accept response"));

        verify(learningSessionService).submitResponse(
                eq(TEMP_USER_ID), eq(resourceId), eq(learningSessionId),
                any(SubmitLearningSessionResponseRequest.class));
    }

    @Test
    void completeSession_shouldReturn200() throws Exception {
        Long resourceId = 10L;
        Long learningSessionId = 900L;
        Instant completedAt = Instant.parse("2026-06-08T13:00:00Z");

        LearningSessionResultDraftResponse resultDraft = new LearningSessionResultDraftResponse();
        resultDraft.setSummary("今回の振り返り内容をもとに、学習内容の要点を整理しました。");
        resultDraft.setConceptTags(List.of("reflection", "understanding", "review"));
        resultDraft.setWeakPointSummary("まだ曖昧な点は、次回の復習で確認してください。");
        resultDraft.setNextAction("今回整理した内容をもとに、重要な概念をもう一度説明できるか確認してください。");
        resultDraft.setAiAssessment("PASSED");
        resultDraft.setGenerationBasis("MVPでは回答ログを保存しないため、固定テンプレートでresultDraftを生成しています。");

        LearningSessionNextActionResponse nextAction = new LearningSessionNextActionResponse();
        nextAction.setType("SAVE_RECORD");

        CompleteLearningSessionResponse response = new CompleteLearningSessionResponse();
        response.setLearningSessionId(learningSessionId);
        response.setResourceId(resourceId);
        response.setSessionType(LearningSessionType.IMMEDIATE_REFLECTION);
        response.setStatus(LearningSessionStatus.COMPLETED);
        response.setResultDraft(resultDraft);
        response.setNextAction(nextAction);
        response.setCompletedAt(completedAt);

        when(learningSessionService.completeSession(TEMP_USER_ID, resourceId, learningSessionId))
                .thenReturn(response);

        mockMvc.perform(post(
                        "/resources/{resourceId}/learning-sessions/{learningSessionId}/complete",
                        resourceId, learningSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learningSessionId").value(900))
                .andExpect(jsonPath("$.resourceId").value(10))
                .andExpect(jsonPath("$.sessionType").value("IMMEDIATE_REFLECTION"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.resultDraft.summary")
                        .value("今回の振り返り内容をもとに、学習内容の要点を整理しました。"))
                .andExpect(jsonPath("$.resultDraft.conceptTags[0]").value("reflection"))
                .andExpect(jsonPath("$.resultDraft.conceptTags[1]").value("understanding"))
                .andExpect(jsonPath("$.resultDraft.conceptTags[2]").value("review"))
                .andExpect(jsonPath("$.resultDraft.weakPointSummary")
                        .value("まだ曖昧な点は、次回の復習で確認してください。"))
                .andExpect(jsonPath("$.resultDraft.nextAction")
                        .value("今回整理した内容をもとに、重要な概念をもう一度説明できるか確認してください。"))
                .andExpect(jsonPath("$.resultDraft.aiAssessment").value("PASSED"))
                .andExpect(jsonPath("$.resultDraft.generationBasis")
                        .value("MVPでは回答ログを保存しないため、固定テンプレートでresultDraftを生成しています。"))
                .andExpect(jsonPath("$.nextAction.type").value("SAVE_RECORD"))
                .andExpect(jsonPath("$.completedAt").value("2026-06-08T13:00:00Z"));

        verify(learningSessionService).completeSession(TEMP_USER_ID, resourceId, learningSessionId);
    }

    @Test
    void completeSession_shouldReturn404WhenSessionNotFound() throws Exception {
        Long resourceId = 10L;
        Long learningSessionId = 900L;

        when(learningSessionService.completeSession(TEMP_USER_ID, resourceId, learningSessionId))
                .thenThrow(new LearningSessionNotFoundException("Learning session not found"));

        mockMvc.perform(post(
                        "/resources/{resourceId}/learning-sessions/{learningSessionId}/complete",
                        resourceId, learningSessionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Learning session not found"));

        verify(learningSessionService).completeSession(TEMP_USER_ID, resourceId, learningSessionId);
    }

    @Test
    void completeSession_shouldReturn400WhenSessionCannotBeCompleted() throws Exception {
        Long resourceId = 10L;
        Long learningSessionId = 900L;

        when(learningSessionService.completeSession(TEMP_USER_ID, resourceId, learningSessionId))
                .thenThrow(new LearningSessionCannotBeCompletedException("Learning session cannot be completed"));

        mockMvc.perform(post(
                        "/resources/{resourceId}/learning-sessions/{learningSessionId}/complete",
                        resourceId, learningSessionId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LEARNING_SESSION_CANNOT_BE_COMPLETED"))
                .andExpect(jsonPath("$.message").value("Learning session cannot be completed"));

        verify(learningSessionService).completeSession(TEMP_USER_ID, resourceId, learningSessionId);
    }
}
