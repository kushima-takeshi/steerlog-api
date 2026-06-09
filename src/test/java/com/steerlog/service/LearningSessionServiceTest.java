package com.steerlog.service;

import com.steerlog.dto.request.StartLearningSessionRequest;
import com.steerlog.dto.response.DiscardLearningSessionResponse;
import com.steerlog.dto.response.LearningSessionResponse;
import com.steerlog.entity.LearningSession;
import com.steerlog.entity.LearningSessionStatus;
import com.steerlog.entity.LearningSessionType;
import com.steerlog.entity.Progress;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.entity.Resource;
import com.steerlog.entity.ResourceType;
import com.steerlog.exception.LevelRequirementNotMetException;
import com.steerlog.exception.LearningSessionCannotBeDiscardedException;
import com.steerlog.exception.LearningSessionNotFoundException;
import com.steerlog.exception.ProgressNotFoundException;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.exception.SessionAlreadyInProgressException;
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

    private LearningSession buildLearningSession(
            Long learningSessionId,
            Long userId,
            Long resourceId,
            LearningSessionType sessionType,
            LearningSessionStatus status,
            Instant completedAt,
            Instant updatedAt) {
        LearningSession session = new LearningSession();
        session.setLearningSessionId(learningSessionId);
        session.setUserId(userId);
        session.setResourceId(resourceId);
        session.setSessionType(sessionType);
        session.setStatus(status);
        session.setCurrentStep(1);
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
