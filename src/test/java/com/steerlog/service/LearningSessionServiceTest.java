package com.steerlog.service;

import com.steerlog.dto.request.SaveLearningSessionRecordRequest;
import com.steerlog.dto.request.StartLearningSessionRequest;
import com.steerlog.dto.request.SubmitLearningSessionResponseRequest;
import com.steerlog.dto.response.CompleteLearningSessionResponse;
import com.steerlog.dto.response.DiscardLearningSessionResponse;
import com.steerlog.dto.response.LearningSessionResponse;
import com.steerlog.dto.response.LearningSessionRecordResponse;
import com.steerlog.dto.response.SubmitLearningSessionResponseResponse;
import com.steerlog.entity.LearningSession;
import com.steerlog.entity.LearningSessionAiAssessment;
import com.steerlog.entity.LearningSessionRecord;
import com.steerlog.entity.LearningSessionStatus;
import com.steerlog.entity.LearningSessionType;
import com.steerlog.entity.Progress;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.entity.Resource;
import com.steerlog.entity.ResourceType;
import com.steerlog.exception.LevelRequirementNotMetException;
import com.steerlog.exception.LearningSessionCannotAcceptResponseException;
import com.steerlog.exception.LearningSessionCannotBeCompletedException;
import com.steerlog.exception.LearningSessionCannotBeDiscardedException;
import com.steerlog.exception.LearningSessionNotFoundException;
import com.steerlog.exception.LearningSessionRecordCannotBeSavedException;
import com.steerlog.exception.ProgressNotFoundException;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.exception.SessionAlreadyInProgressException;
import com.steerlog.repository.LearningSessionRecordRepository;
import com.steerlog.repository.LearningSessionRepository;
import com.steerlog.repository.ProgressRepository;
import com.steerlog.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningSessionServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ProgressRepository progressRepository;

    @Mock
    private LearningSessionRepository learningSessionRepository;

    @Mock
    private LearningSessionRecordRepository learningSessionRecordRepository;

    @InjectMocks
    private LearningSessionService learningSessionService;

    @Test
    void startSession_shouldStartImmediateReflection() {
        Long userId = 1L;
        Long resourceId = 10L;

        StartLearningSessionRequest request = new StartLearningSessionRequest();
        request.setSessionType(LearningSessionType.IMMEDIATE_REFLECTION);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(buildResource(resourceId, userId)));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(buildProgress(300L, userId, resourceId, 1)));
        when(learningSessionRepository.existsByUserIdAndResourceIdAndSessionTypeAndStatusIn(
                userId,
                resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                List.of(LearningSessionStatus.IN_PROGRESS, LearningSessionStatus.COMPLETED)))
                .thenReturn(false);
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> {
            LearningSession session = invocation.getArgument(0);
            session.setLearningSessionId(700L);
            return session;
        });

        LearningSessionResponse response = learningSessionService.startSession(userId, resourceId, request);

        assertThat(response.getLearningSessionId()).isEqualTo(700L);
        assertThat(response.getResourceId()).isEqualTo(resourceId);
        assertThat(response.getSessionType()).isEqualTo(LearningSessionType.IMMEDIATE_REFLECTION);
        assertThat(response.getStatus()).isEqualTo(LearningSessionStatus.IN_PROGRESS);
        assertThat(response.getAiPrompt())
                .isEqualTo("このResourceで学んだ内容を、自分の言葉で説明してください。");
        assertThat(response.getStep().getCurrentStep()).isEqualTo(1);
        assertThat(response.getStep().getTotalSteps()).isEqualTo(3);
        assertThat(response.getStep().getIsFinalStep()).isFalse();
        assertThat(response.getNextAction().getType()).isEqualTo("SUBMIT_RESPONSE");
        assertThat(response.getStartedAt()).isNotNull();
    }

    @Test
    void startSession_shouldRejectDelayedRecallWhenCurrentLevelBelow2() {
        Long userId = 1L;
        Long resourceId = 10L;

        StartLearningSessionRequest request = new StartLearningSessionRequest();
        request.setSessionType(LearningSessionType.DELAYED_RECALL);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(buildResource(resourceId, userId)));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(buildProgress(300L, userId, resourceId, 1)));

        assertThatThrownBy(() -> learningSessionService.startSession(userId, resourceId, request))
                .isInstanceOf(LevelRequirementNotMetException.class)
                .hasMessage("Level requirement not met");

        verify(learningSessionRepository, never()).existsByUserIdAndResourceIdAndSessionTypeAndStatusIn(
                any(), any(), any(), any());
        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    @Test
    void startSession_shouldStartDelayedRecallWhenCurrentLevelIs2OrAbove() {
        Long userId = 1L;
        Long resourceId = 10L;

        StartLearningSessionRequest request = new StartLearningSessionRequest();
        request.setSessionType(LearningSessionType.DELAYED_RECALL);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(buildResource(resourceId, userId)));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(buildProgress(300L, userId, resourceId, 2)));
        when(learningSessionRepository.existsByUserIdAndResourceIdAndSessionTypeAndStatusIn(
                userId,
                resourceId,
                LearningSessionType.DELAYED_RECALL,
                List.of(LearningSessionStatus.IN_PROGRESS, LearningSessionStatus.COMPLETED)))
                .thenReturn(false);
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> {
            LearningSession session = invocation.getArgument(0);
            session.setLearningSessionId(701L);
            return session;
        });

        LearningSessionResponse response = learningSessionService.startSession(userId, resourceId, request);

        assertThat(response.getSessionType()).isEqualTo(LearningSessionType.DELAYED_RECALL);
        assertThat(response.getAiPrompt())
                .isEqualTo("以前学んだこのResourceの内容を、今覚えている範囲で説明してください。");
    }

    @Test
    void startSession_shouldRejectWhenActiveSessionExists() {
        Long userId = 1L;
        Long resourceId = 10L;

        StartLearningSessionRequest request = new StartLearningSessionRequest();
        request.setSessionType(LearningSessionType.IMMEDIATE_REFLECTION);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(buildResource(resourceId, userId)));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(buildProgress(300L, userId, resourceId, 1)));
        when(learningSessionRepository.existsByUserIdAndResourceIdAndSessionTypeAndStatusIn(
                eq(userId),
                eq(resourceId),
                eq(LearningSessionType.IMMEDIATE_REFLECTION),
                eq(List.of(LearningSessionStatus.IN_PROGRESS, LearningSessionStatus.COMPLETED))))
                .thenReturn(true);

        assertThatThrownBy(() -> learningSessionService.startSession(userId, resourceId, request))
                .isInstanceOf(SessionAlreadyInProgressException.class)
                .hasMessage("Session already in progress");

        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    @Test
    void startSession_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;

        StartLearningSessionRequest request = new StartLearningSessionRequest();
        request.setSessionType(LearningSessionType.IMMEDIATE_REFLECTION);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningSessionService.startSession(userId, resourceId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(progressRepository, never()).findByUserIdAndResourceId(any(), any());
        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    @Test
    void startSession_shouldCreateLearningSessionWithCorrectInitialValues() {
        Long userId = 1L;
        Long resourceId = 10L;

        StartLearningSessionRequest request = new StartLearningSessionRequest();
        request.setSessionType(LearningSessionType.IMMEDIATE_REFLECTION);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(buildResource(resourceId, userId)));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(buildProgress(300L, userId, resourceId, 1)));
        when(learningSessionRepository.existsByUserIdAndResourceIdAndSessionTypeAndStatusIn(
                userId,
                resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                List.of(LearningSessionStatus.IN_PROGRESS, LearningSessionStatus.COMPLETED)))
                .thenReturn(false);
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> {
            LearningSession session = invocation.getArgument(0);
            session.setLearningSessionId(702L);
            return session;
        });

        learningSessionService.startSession(userId, resourceId, request);

        ArgumentCaptor<LearningSession> sessionCaptor = ArgumentCaptor.forClass(LearningSession.class);
        verify(learningSessionRepository).save(sessionCaptor.capture());

        LearningSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getUserId()).isEqualTo(userId);
        assertThat(savedSession.getResourceId()).isEqualTo(resourceId);
        assertThat(savedSession.getSessionType()).isEqualTo(LearningSessionType.IMMEDIATE_REFLECTION);
        assertThat(savedSession.getStatus()).isEqualTo(LearningSessionStatus.IN_PROGRESS);
        assertThat(savedSession.getCurrentStep()).isEqualTo(1);
        assertThat(savedSession.getTotalSteps()).isEqualTo(3);
        assertThat(savedSession.getCompletedAt()).isNull();
        assertThat(savedSession.getStartedAt()).isNotNull();
        assertThat(savedSession.getCreatedAt()).isNotNull();
        assertThat(savedSession.getUpdatedAt()).isNotNull();
    }

    @Test
    void discardSession_shouldDiscardInProgressSession() {
        Long userId = 1L;
        Long learningSessionId = 700L;
        Long resourceId = 10L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                1,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DiscardLearningSessionResponse response =
                learningSessionService.discardSession(userId, resourceId, learningSessionId);

        ArgumentCaptor<LearningSession> sessionCaptor = ArgumentCaptor.forClass(LearningSession.class);
        verify(learningSessionRepository).save(sessionCaptor.capture());

        LearningSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getStatus()).isEqualTo(LearningSessionStatus.DISCARDED);
        assertThat(savedSession.getCompletedAt()).isNull();

        assertThat(response.getLearningSessionId()).isEqualTo(learningSessionId);
        assertThat(response.getResourceId()).isEqualTo(resourceId);
        assertThat(response.getSessionType()).isEqualTo(LearningSessionType.IMMEDIATE_REFLECTION);
        assertThat(response.getStatus()).isEqualTo(LearningSessionStatus.DISCARDED);
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void discardSession_shouldDiscardCompletedSession() {
        Long userId = 1L;
        Long learningSessionId = 701L;
        Long resourceId = 10L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");
        Instant completedAt = Instant.parse("2026-06-08T11:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.COMPLETED,
                1,
                completedAt,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DiscardLearningSessionResponse response =
                learningSessionService.discardSession(userId, resourceId, learningSessionId);

        ArgumentCaptor<LearningSession> sessionCaptor = ArgumentCaptor.forClass(LearningSession.class);
        verify(learningSessionRepository).save(sessionCaptor.capture());

        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(LearningSessionStatus.DISCARDED);
        assertThat(sessionCaptor.getValue().getCompletedAt()).isEqualTo(completedAt);
        assertThat(response.getStatus()).isEqualTo(LearningSessionStatus.DISCARDED);
    }

    @Test
    void discardSession_shouldRejectRecordSavedSession() {
        Long userId = 1L;
        Long learningSessionId = 702L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, 10L,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.RECORD_SAVED,
                1,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, 10L))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> learningSessionService.discardSession(userId, 10L, learningSessionId))
                .isInstanceOf(LearningSessionCannotBeDiscardedException.class)
                .hasMessage("Learning session cannot be discarded");

        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    @Test
    void discardSession_shouldRejectAlreadyDiscardedSession() {
        Long userId = 1L;
        Long learningSessionId = 703L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, 10L,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.DISCARDED,
                1,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, 10L))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> learningSessionService.discardSession(userId, 10L, learningSessionId))
                .isInstanceOf(LearningSessionCannotBeDiscardedException.class)
                .hasMessage("Learning session cannot be discarded");

        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    @Test
    void discardSession_shouldThrowLearningSessionNotFoundExceptionWhenSessionNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 704L;

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningSessionService.discardSession(userId, resourceId, learningSessionId))
                .isInstanceOf(LearningSessionNotFoundException.class)
                .hasMessage("Learning session not found");

        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    @Test
    void discardSession_shouldNotChangeCompletedAt() {
        Long userId = 1L;
        Long learningSessionId = 705L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");
        Instant completedAt = Instant.parse("2026-06-08T11:00:00Z");

        LearningSession inProgressSession = buildLearningSession(
                learningSessionId, userId, 10L,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                1,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, 10L))
                .thenReturn(Optional.of(inProgressSession));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        learningSessionService.discardSession(userId, 10L, learningSessionId);

        ArgumentCaptor<LearningSession> sessionCaptor = ArgumentCaptor.forClass(LearningSession.class);
        verify(learningSessionRepository).save(sessionCaptor.capture());

        assertThat(sessionCaptor.getValue().getCompletedAt()).isNull();
    }

    @Test
    void discardSession_shouldUpdateUpdatedAt() {
        Long userId = 1L;
        Long learningSessionId = 706L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, 10L,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                1,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, 10L))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        learningSessionService.discardSession(userId, 10L, learningSessionId);

        ArgumentCaptor<LearningSession> sessionCaptor = ArgumentCaptor.forClass(LearningSession.class);
        verify(learningSessionRepository).save(sessionCaptor.capture());

        assertThat(sessionCaptor.getValue().getUpdatedAt()).isNotNull();
        assertThat(sessionCaptor.getValue().getUpdatedAt()).isAfter(before);
    }

    @Test
    void submitResponse_shouldAdvanceCurrentStep() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 800L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                1,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubmitLearningSessionResponseRequest request = new SubmitLearningSessionResponseRequest();
        request.setResponseText("REST APIの基本を説明した");

        SubmitLearningSessionResponseResponse response = learningSessionService.submitResponse(
                userId, resourceId, learningSessionId, request);

        ArgumentCaptor<LearningSession> sessionCaptor = ArgumentCaptor.forClass(LearningSession.class);
        verify(learningSessionRepository).save(sessionCaptor.capture());

        assertThat(sessionCaptor.getValue().getCurrentStep()).isEqualTo(2);
        assertThat(response.getStep().getCurrentStep()).isEqualTo(2);
    }

    @Test
    void submitResponse_shouldKeepStatusInProgress() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 801L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                1,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubmitLearningSessionResponseResponse response = learningSessionService.submitResponse(
                userId, resourceId, learningSessionId, buildSubmitRequest());

        assertThat(response.getStatus()).isEqualTo(LearningSessionStatus.IN_PROGRESS);
    }

    @Test
    void submitResponse_shouldNotPersistResponseText() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 802L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                1,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        learningSessionService.submitResponse(
                userId, resourceId, learningSessionId, buildSubmitRequest());

        verify(learningSessionRepository).save(any(LearningSession.class));
        verify(resourceRepository, never()).save(any());
        verify(progressRepository, never()).save(any());
    }

    @Test
    void submitResponse_shouldUpdateUpdatedAt() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 803L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                1,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        learningSessionService.submitResponse(
                userId, resourceId, learningSessionId, buildSubmitRequest());

        ArgumentCaptor<LearningSession> sessionCaptor = ArgumentCaptor.forClass(LearningSession.class);
        verify(learningSessionRepository).save(sessionCaptor.capture());

        assertThat(sessionCaptor.getValue().getUpdatedAt()).isNotNull();
        assertThat(sessionCaptor.getValue().getUpdatedAt()).isAfter(before);
    }

    @Test
    void submitResponse_shouldNotChangeCompletedAt() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 804L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");
        Instant completedAt = Instant.parse("2026-06-08T11:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                1,
                completedAt,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        learningSessionService.submitResponse(
                userId, resourceId, learningSessionId, buildSubmitRequest());

        ArgumentCaptor<LearningSession> sessionCaptor = ArgumentCaptor.forClass(LearningSession.class);
        verify(learningSessionRepository).save(sessionCaptor.capture());

        assertThat(sessionCaptor.getValue().getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void submitResponse_shouldReturnCorrectIsFinalStep() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 805L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession sessionStep1 = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                1,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(sessionStep1));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubmitLearningSessionResponseResponse responseAfterStep1 = learningSessionService.submitResponse(
                userId, resourceId, learningSessionId, buildSubmitRequest());

        assertThat(responseAfterStep1.getStep().getIsFinalStep()).isFalse();

        LearningSession sessionStep2 = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                2,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(sessionStep2));

        SubmitLearningSessionResponseResponse responseAfterStep2 = learningSessionService.submitResponse(
                userId, resourceId, learningSessionId, buildSubmitRequest());

        assertThat(responseAfterStep2.getStep().getIsFinalStep()).isTrue();
    }

    @Test
    void submitResponse_shouldReturnSubmitResponseNextActionWhenNotFinalStep() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 806L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                1,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubmitLearningSessionResponseResponse response = learningSessionService.submitResponse(
                userId, resourceId, learningSessionId, buildSubmitRequest());

        assertThat(response.getNextAction().getType()).isEqualTo("SUBMIT_RESPONSE");
    }

    @Test
    void submitResponse_shouldReturnCompleteSessionNextActionWhenFinalStep() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 807L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                2,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubmitLearningSessionResponseResponse response = learningSessionService.submitResponse(
                userId, resourceId, learningSessionId, buildSubmitRequest());

        assertThat(response.getNextAction().getType()).isEqualTo("COMPLETE_SESSION");
    }

    @Test
    void submitResponse_shouldRejectCompletedSession() {
        assertSubmitResponseRejectedForStatus(LearningSessionStatus.COMPLETED);
    }

    @Test
    void submitResponse_shouldRejectRecordSavedSession() {
        assertSubmitResponseRejectedForStatus(LearningSessionStatus.RECORD_SAVED);
    }

    @Test
    void submitResponse_shouldRejectDiscardedSession() {
        assertSubmitResponseRejectedForStatus(LearningSessionStatus.DISCARDED);
    }

    @Test
    void submitResponse_shouldRejectWhenCurrentStepIsAtOrBeyondTotalSteps() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 808L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                3,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> learningSessionService.submitResponse(
                userId, resourceId, learningSessionId, buildSubmitRequest()))
                .isInstanceOf(LearningSessionCannotAcceptResponseException.class)
                .hasMessage("Learning session cannot accept response");

        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    @Test
    void submitResponse_shouldThrowLearningSessionNotFoundExceptionWhenSessionNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 809L;

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningSessionService.submitResponse(
                userId, resourceId, learningSessionId, buildSubmitRequest()))
                .isInstanceOf(LearningSessionNotFoundException.class)
                .hasMessage("Learning session not found");

        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    private void assertSubmitResponseRejectedForStatus(LearningSessionStatus status) {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 810L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                status,
                1,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> learningSessionService.submitResponse(
                userId, resourceId, learningSessionId, buildSubmitRequest()))
                .isInstanceOf(LearningSessionCannotAcceptResponseException.class)
                .hasMessage("Learning session cannot accept response");

        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    @Test
    void completeSession_shouldCompleteInProgressSessionAtFinalStep() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 900L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                3,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompleteLearningSessionResponse response =
                learningSessionService.completeSession(userId, resourceId, learningSessionId);

        ArgumentCaptor<LearningSession> sessionCaptor = ArgumentCaptor.forClass(LearningSession.class);
        verify(learningSessionRepository).save(sessionCaptor.capture());

        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(LearningSessionStatus.COMPLETED);
        assertThat(response.getStatus()).isEqualTo(LearningSessionStatus.COMPLETED);
    }

    @Test
    void completeSession_shouldSetCompletedAt() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 901L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                3,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompleteLearningSessionResponse response =
                learningSessionService.completeSession(userId, resourceId, learningSessionId);

        assertThat(response.getCompletedAt()).isNotNull();
        assertThat(response.getCompletedAt()).isAfter(before);
    }

    @Test
    void completeSession_shouldUpdateUpdatedAt() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 902L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                3,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        learningSessionService.completeSession(userId, resourceId, learningSessionId);

        ArgumentCaptor<LearningSession> sessionCaptor = ArgumentCaptor.forClass(LearningSession.class);
        verify(learningSessionRepository).save(sessionCaptor.capture());

        assertThat(sessionCaptor.getValue().getUpdatedAt()).isNotNull();
        assertThat(sessionCaptor.getValue().getUpdatedAt()).isAfter(before);
    }

    @Test
    void completeSession_shouldNotChangeCurrentStepOrTotalSteps() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 903L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                3,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        learningSessionService.completeSession(userId, resourceId, learningSessionId);

        ArgumentCaptor<LearningSession> sessionCaptor = ArgumentCaptor.forClass(LearningSession.class);
        verify(learningSessionRepository).save(sessionCaptor.capture());

        assertThat(sessionCaptor.getValue().getCurrentStep()).isEqualTo(3);
        assertThat(sessionCaptor.getValue().getTotalSteps()).isEqualTo(3);
    }

    @Test
    void completeSession_shouldReturnResultDraft() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 904L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                3,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompleteLearningSessionResponse response =
                learningSessionService.completeSession(userId, resourceId, learningSessionId);

        assertThat(response.getResultDraft()).isNotNull();
        assertThat(response.getResultDraft().getSummary())
                .isEqualTo("今回の振り返り内容をもとに、学習内容の要点を整理しました。");
        assertThat(response.getResultDraft().getConceptTags())
                .containsExactly("reflection", "understanding", "review");
        assertThat(response.getResultDraft().getWeakPointSummary())
                .isEqualTo("まだ曖昧な点は、次回の復習で確認してください。");
        assertThat(response.getResultDraft().getNextAction())
                .isEqualTo("今回整理した内容をもとに、重要な概念をもう一度説明できるか確認してください。");
        assertThat(response.getResultDraft().getAiAssessment()).isEqualTo("PASSED");
        assertThat(response.getResultDraft().getGenerationBasis())
                .isEqualTo("MVPでは回答ログを保存しないため、固定テンプレートでresultDraftを生成しています。");
    }

    @Test
    void completeSession_shouldReturnDelayedRecallResultDraft() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 905L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.DELAYED_RECALL,
                LearningSessionStatus.IN_PROGRESS,
                3,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompleteLearningSessionResponse response =
                learningSessionService.completeSession(userId, resourceId, learningSessionId);

        assertThat(response.getResultDraft().getSummary())
                .isEqualTo("今回の想起内容をもとに、記憶に残っている内容を整理しました。");
        assertThat(response.getResultDraft().getConceptTags())
                .containsExactly("recall", "retention", "review");
    }

    @Test
    void completeSession_shouldReturnSaveRecordNextAction() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 906L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                3,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompleteLearningSessionResponse response =
                learningSessionService.completeSession(userId, resourceId, learningSessionId);

        assertThat(response.getNextAction().getType()).isEqualTo("SAVE_RECORD");
    }

    @Test
    void completeSession_shouldRejectWhenCurrentStepBelowTotalSteps() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 907L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                2,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> learningSessionService.completeSession(userId, resourceId, learningSessionId))
                .isInstanceOf(LearningSessionCannotBeCompletedException.class)
                .hasMessage("Learning session cannot be completed");

        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    @Test
    void completeSession_shouldRejectCompletedSession() {
        assertCompleteSessionRejectedForStatus(LearningSessionStatus.COMPLETED);
    }

    @Test
    void completeSession_shouldRejectRecordSavedSession() {
        assertCompleteSessionRejectedForStatus(LearningSessionStatus.RECORD_SAVED);
    }

    @Test
    void completeSession_shouldRejectDiscardedSession() {
        assertCompleteSessionRejectedForStatus(LearningSessionStatus.DISCARDED);
    }

    @Test
    void completeSession_shouldThrowLearningSessionNotFoundExceptionWhenSessionNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 908L;

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningSessionService.completeSession(userId, resourceId, learningSessionId))
                .isInstanceOf(LearningSessionNotFoundException.class)
                .hasMessage("Learning session not found");

        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    @Test
    void completeSession_shouldNotPersistResultDraftToDatabase() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 909L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.IN_PROGRESS,
                3,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        learningSessionService.completeSession(userId, resourceId, learningSessionId);

        verify(learningSessionRepository).save(any(LearningSession.class));
        verify(resourceRepository, never()).save(any());
        verify(progressRepository, never()).save(any());
    }

    private void assertCompleteSessionRejectedForStatus(LearningSessionStatus status) {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 910L;
        Instant before = Instant.parse("2026-06-08T10:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                status,
                3,
                null,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> learningSessionService.completeSession(userId, resourceId, learningSessionId))
                .isInstanceOf(LearningSessionCannotBeCompletedException.class)
                .hasMessage("Learning session cannot be completed");

        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    @Test
    void saveRecord_shouldSaveLearningSessionRecordFromCompletedSession() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 1000L;
        Instant completedAt = Instant.parse("2026-06-08T13:00:00Z");
        Instant before = Instant.parse("2026-06-08T12:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.COMPLETED,
                3,
                completedAt,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRecordRepository.existsByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(false);
        when(learningSessionRecordRepository.save(any(LearningSessionRecord.class))).thenAnswer(invocation -> {
            LearningSessionRecord record = invocation.getArgument(0);
            record.setLearningSessionRecordId(2000L);
            return record;
        });
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LearningSessionRecordResponse response = learningSessionService.saveRecord(
                userId, resourceId, learningSessionId, buildSaveRecordRequest());

        assertThat(response.getLearningSessionRecordId()).isEqualTo(2000L);
        assertThat(response.getResourceId()).isEqualTo(resourceId);
        assertThat(response.getLearningSessionId()).isEqualTo(learningSessionId);
        assertThat(response.getSessionType()).isEqualTo(LearningSessionType.IMMEDIATE_REFLECTION);
        assertThat(response.getSummary()).isEqualTo("学習内容の要点まとめ");
        assertThat(response.getAiAssessment()).isEqualTo(LearningSessionAiAssessment.PASSED);
    }

    @Test
    void saveRecord_shouldSetLearningSessionStatusToRecordSaved() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 1001L;
        Instant completedAt = Instant.parse("2026-06-08T13:00:00Z");
        Instant before = Instant.parse("2026-06-08T12:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.COMPLETED,
                3,
                completedAt,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRecordRepository.existsByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(false);
        when(learningSessionRecordRepository.save(any(LearningSessionRecord.class))).thenAnswer(invocation -> {
            LearningSessionRecord record = invocation.getArgument(0);
            record.setLearningSessionRecordId(2001L);
            return record;
        });
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        learningSessionService.saveRecord(userId, resourceId, learningSessionId, buildSaveRecordRequest());

        ArgumentCaptor<LearningSession> sessionCaptor = ArgumentCaptor.forClass(LearningSession.class);
        verify(learningSessionRepository).save(sessionCaptor.capture());

        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(LearningSessionStatus.RECORD_SAVED);
    }

    @Test
    void saveRecord_shouldUpdateLearningSessionUpdatedAt() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 1002L;
        Instant completedAt = Instant.parse("2026-06-08T13:00:00Z");
        Instant before = Instant.parse("2026-06-08T12:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.COMPLETED,
                3,
                completedAt,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRecordRepository.existsByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(false);
        when(learningSessionRecordRepository.save(any(LearningSessionRecord.class))).thenAnswer(invocation -> {
            LearningSessionRecord record = invocation.getArgument(0);
            record.setLearningSessionRecordId(2002L);
            return record;
        });
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        learningSessionService.saveRecord(userId, resourceId, learningSessionId, buildSaveRecordRequest());

        ArgumentCaptor<LearningSession> sessionCaptor = ArgumentCaptor.forClass(LearningSession.class);
        verify(learningSessionRepository).save(sessionCaptor.capture());

        assertThat(sessionCaptor.getValue().getUpdatedAt()).isNotNull();
        assertThat(sessionCaptor.getValue().getUpdatedAt()).isAfter(before);
    }

    @Test
    void saveRecord_shouldNotChangeLearningSessionCompletedAt() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 1003L;
        Instant completedAt = Instant.parse("2026-06-08T13:00:00Z");
        Instant before = Instant.parse("2026-06-08T12:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.COMPLETED,
                3,
                completedAt,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRecordRepository.existsByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(false);
        when(learningSessionRecordRepository.save(any(LearningSessionRecord.class))).thenAnswer(invocation -> {
            LearningSessionRecord record = invocation.getArgument(0);
            record.setLearningSessionRecordId(2003L);
            return record;
        });
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        learningSessionService.saveRecord(userId, resourceId, learningSessionId, buildSaveRecordRequest());

        ArgumentCaptor<LearningSession> sessionCaptor = ArgumentCaptor.forClass(LearningSession.class);
        verify(learningSessionRepository).save(sessionCaptor.capture());

        assertThat(sessionCaptor.getValue().getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void saveRecord_shouldNotUpdateProgressCurrentLevel() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 1004L;
        Instant completedAt = Instant.parse("2026-06-08T13:00:00Z");
        Instant before = Instant.parse("2026-06-08T12:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.COMPLETED,
                3,
                completedAt,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRecordRepository.existsByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(false);
        when(learningSessionRecordRepository.save(any(LearningSessionRecord.class))).thenAnswer(invocation -> {
            LearningSessionRecord record = invocation.getArgument(0);
            record.setLearningSessionRecordId(2004L);
            return record;
        });
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        learningSessionService.saveRecord(userId, resourceId, learningSessionId, buildSaveRecordRequest());

        verify(progressRepository, never()).save(any(Progress.class));
        verify(progressRepository, never()).findByUserIdAndResourceId(any(), any());
    }

    @Test
    void saveRecord_shouldRejectInProgressSession() {
        assertSaveRecordRejectedForStatus(LearningSessionStatus.IN_PROGRESS);
    }

    @Test
    void saveRecord_shouldRejectRecordSavedSession() {
        assertSaveRecordRejectedForStatus(LearningSessionStatus.RECORD_SAVED);
    }

    @Test
    void saveRecord_shouldRejectDiscardedSession() {
        assertSaveRecordRejectedForStatus(LearningSessionStatus.DISCARDED);
    }

    @Test
    void saveRecord_shouldRejectOffTopicAssessment() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 1005L;
        Instant completedAt = Instant.parse("2026-06-08T13:00:00Z");
        Instant before = Instant.parse("2026-06-08T12:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.COMPLETED,
                3,
                completedAt,
                before);

        SaveLearningSessionRecordRequest request = buildSaveRecordRequest();
        request.setAiAssessment(LearningSessionAiAssessment.OFF_TOPIC);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> learningSessionService.saveRecord(
                userId, resourceId, learningSessionId, request))
                .isInstanceOf(LearningSessionRecordCannotBeSavedException.class)
                .hasMessage("Learning session record cannot be saved");

        verify(learningSessionRecordRepository, never()).save(any(LearningSessionRecord.class));
        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    @Test
    void saveRecord_shouldRejectWhenRecordAlreadyExists() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 1006L;
        Instant completedAt = Instant.parse("2026-06-08T13:00:00Z");
        Instant before = Instant.parse("2026-06-08T12:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.COMPLETED,
                3,
                completedAt,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRecordRepository.existsByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(true);

        assertThatThrownBy(() -> learningSessionService.saveRecord(
                userId, resourceId, learningSessionId, buildSaveRecordRequest()))
                .isInstanceOf(LearningSessionRecordCannotBeSavedException.class)
                .hasMessage("Learning session record cannot be saved");

        verify(learningSessionRecordRepository, never()).save(any(LearningSessionRecord.class));
        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    @Test
    void saveRecord_shouldThrowLearningSessionNotFoundExceptionWhenSessionNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 1007L;

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningSessionService.saveRecord(
                userId, resourceId, learningSessionId, buildSaveRecordRequest()))
                .isInstanceOf(LearningSessionNotFoundException.class)
                .hasMessage("Learning session not found");

        verify(learningSessionRecordRepository, never()).save(any(LearningSessionRecord.class));
        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    @Test
    void saveRecord_shouldStoreConceptTagsAsCommaSeparatedString() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 1008L;
        Instant completedAt = Instant.parse("2026-06-08T13:00:00Z");
        Instant before = Instant.parse("2026-06-08T12:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.COMPLETED,
                3,
                completedAt,
                before);

        SaveLearningSessionRecordRequest request = buildSaveRecordRequest();
        request.setConceptTags(List.of("reflection", "understanding", "review"));

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRecordRepository.existsByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(false);
        when(learningSessionRecordRepository.save(any(LearningSessionRecord.class))).thenAnswer(invocation -> {
            LearningSessionRecord record = invocation.getArgument(0);
            record.setLearningSessionRecordId(2008L);
            return record;
        });
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        learningSessionService.saveRecord(userId, resourceId, learningSessionId, request);

        ArgumentCaptor<LearningSessionRecord> recordCaptor = ArgumentCaptor.forClass(LearningSessionRecord.class);
        verify(learningSessionRecordRepository).save(recordCaptor.capture());

        assertThat(recordCaptor.getValue().getConceptTags()).isEqualTo("reflection,understanding,review");
    }

    @Test
    void saveRecord_shouldReturnConceptTagsAsListInResponse() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 1009L;
        Instant completedAt = Instant.parse("2026-06-08T13:00:00Z");
        Instant before = Instant.parse("2026-06-08T12:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                LearningSessionStatus.COMPLETED,
                3,
                completedAt,
                before);

        SaveLearningSessionRecordRequest request = buildSaveRecordRequest();
        request.setConceptTags(List.of("reflection", "understanding", "review"));

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));
        when(learningSessionRecordRepository.existsByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(false);
        when(learningSessionRecordRepository.save(any(LearningSessionRecord.class))).thenAnswer(invocation -> {
            LearningSessionRecord record = invocation.getArgument(0);
            record.setLearningSessionRecordId(2009L);
            return record;
        });
        when(learningSessionRepository.save(any(LearningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LearningSessionRecordResponse response = learningSessionService.saveRecord(
                userId, resourceId, learningSessionId, request);

        assertThat(response.getConceptTags()).containsExactly("reflection", "understanding", "review");
    }

    private void assertSaveRecordRejectedForStatus(LearningSessionStatus status) {
        Long userId = 1L;
        Long resourceId = 10L;
        Long learningSessionId = 1010L;
        Instant completedAt = Instant.parse("2026-06-08T13:00:00Z");
        Instant before = Instant.parse("2026-06-08T12:00:00Z");

        LearningSession session = buildLearningSession(
                learningSessionId, userId, resourceId,
                LearningSessionType.IMMEDIATE_REFLECTION,
                status,
                3,
                completedAt,
                before);

        when(learningSessionRepository.findByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> learningSessionService.saveRecord(
                userId, resourceId, learningSessionId, buildSaveRecordRequest()))
                .isInstanceOf(LearningSessionRecordCannotBeSavedException.class)
                .hasMessage("Learning session record cannot be saved");

        verify(learningSessionRecordRepository, never()).save(any(LearningSessionRecord.class));
        verify(learningSessionRepository, never()).save(any(LearningSession.class));
    }

    private SaveLearningSessionRecordRequest buildSaveRecordRequest() {
        SaveLearningSessionRecordRequest request = new SaveLearningSessionRecordRequest();
        request.setSummary("学習内容の要点まとめ");
        request.setWeakPointSummary("まだ曖昧な点あり");
        request.setNextAction("次回復習する");
        request.setAiAssessment(LearningSessionAiAssessment.PASSED);
        return request;
    }

    private SubmitLearningSessionResponseRequest buildSubmitRequest() {
        SubmitLearningSessionResponseRequest request = new SubmitLearningSessionResponseRequest();
        request.setResponseText("回答本文");
        return request;
    }

    private LearningSession buildLearningSession(
            Long learningSessionId,
            Long userId,
            Long resourceId,
            LearningSessionType sessionType,
            LearningSessionStatus status,
            int currentStep,
            Instant completedAt,
            Instant updatedAt) {
        LearningSession session = new LearningSession();
        session.setLearningSessionId(learningSessionId);
        session.setUserId(userId);
        session.setResourceId(resourceId);
        session.setSessionType(sessionType);
        session.setStatus(status);
        session.setCurrentStep(currentStep);
        session.setTotalSteps(3);
        session.setStartedAt(updatedAt);
        session.setCompletedAt(completedAt);
        session.setCreatedAt(updatedAt);
        session.setUpdatedAt(updatedAt);
        return session;
    }

    private Resource buildResource(Long resourceId, Long userId) {
        Instant now = Instant.parse("2026-06-01T10:00:00Z");
        Resource resource = new Resource();
        resource.setResourceId(resourceId);
        resource.setUserId(userId);
        resource.setResourceType(ResourceType.BOOK);
        resource.setTitle("Webを支える技術");
        resource.setCreatedAt(now);
        resource.setUpdatedAt(now);
        return resource;
    }

    private Progress buildProgress(Long progressId, Long userId, Long resourceId, int currentLevel) {
        Instant now = Instant.parse("2026-06-01T10:00:00Z");
        Progress progress = new Progress();
        progress.setProgressId(progressId);
        progress.setUserId(userId);
        progress.setResourceId(resourceId);
        progress.setStatus(ProgressStatus.IN_PROGRESS);
        progress.setCurrentLevel(currentLevel);
        progress.setCreatedAt(now);
        progress.setUpdatedAt(now);
        return progress;
    }
}
