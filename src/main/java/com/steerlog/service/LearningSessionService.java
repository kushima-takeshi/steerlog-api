package com.steerlog.service;

import com.steerlog.dto.request.StartLearningSessionRequest;
import com.steerlog.dto.request.SubmitLearningSessionResponseRequest;
import com.steerlog.dto.response.DiscardLearningSessionResponse;
import com.steerlog.dto.response.SubmitLearningSessionResponseResponse;
import com.steerlog.dto.response.LearningSessionNextActionResponse;
import com.steerlog.dto.response.LearningSessionResponse;
import com.steerlog.dto.response.LearningSessionStepResponse;
import com.steerlog.entity.LearningSession;
import com.steerlog.entity.LearningSessionStatus;
import com.steerlog.entity.LearningSessionType;
import com.steerlog.entity.Progress;
import com.steerlog.exception.LevelRequirementNotMetException;
import com.steerlog.exception.LearningSessionCannotAcceptResponseException;
import com.steerlog.exception.LearningSessionCannotBeDiscardedException;
import com.steerlog.exception.LearningSessionNotFoundException;
import com.steerlog.exception.ProgressNotFoundException;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.exception.SessionAlreadyInProgressException;
import com.steerlog.repository.LearningSessionRepository;
import com.steerlog.repository.ProgressRepository;
import com.steerlog.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class LearningSessionService {

    private static final int TOTAL_STEPS = 3;
    private static final String NEXT_ACTION_SUBMIT_RESPONSE = "SUBMIT_RESPONSE";
    private static final String NEXT_ACTION_COMPLETE_SESSION = "COMPLETE_SESSION";

    private static final String IMMEDIATE_REFLECTION_PROMPT_STEP_1 =
            "このResourceで学んだ内容を、自分の言葉で説明してください。";
    private static final String IMMEDIATE_REFLECTION_PROMPT_STEP_2 =
            "なぜそれが重要だと思ったか、具体例を交えて説明してください。";
    private static final String IMMEDIATE_REFLECTION_PROMPT_STEP_3 =
            "まだ曖昧な点や、次に復習したい点を説明してください。";

    private static final String DELAYED_RECALL_PROMPT_STEP_1 =
            "以前学んだこのResourceの内容を、今覚えている範囲で説明してください。";
    private static final String DELAYED_RECALL_PROMPT_STEP_2 =
            "その内容を実務や実装にどう使えるか説明してください。";
    private static final String DELAYED_RECALL_PROMPT_STEP_3 =
            "思い出せなかった点や、理解が曖昧だと感じた点を説明してください。";

    private final ResourceRepository resourceRepository;
    private final ProgressRepository progressRepository;
    private final LearningSessionRepository learningSessionRepository;

    public LearningSessionService(
            ResourceRepository resourceRepository,
            ProgressRepository progressRepository,
            LearningSessionRepository learningSessionRepository) {
        this.resourceRepository = resourceRepository;
        this.progressRepository = progressRepository;
        this.learningSessionRepository = learningSessionRepository;
    }

    @Transactional
    public LearningSessionResponse startSession(
            Long userId, Long resourceId, StartLearningSessionRequest request) {
        resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        Progress progress = progressRepository
                .findByUserIdAndResourceId(userId, resourceId)
                .orElseThrow(() -> new ProgressNotFoundException("Progress not found"));

        LearningSessionType sessionType = request.getSessionType();

        if (sessionType == LearningSessionType.DELAYED_RECALL && progress.getCurrentLevel() < 2) {
            throw new LevelRequirementNotMetException("Level requirement not met");
        }

        boolean activeSessionExists = learningSessionRepository
                .existsByUserIdAndResourceIdAndSessionTypeAndStatusIn(
                        userId,
                        resourceId,
                        sessionType,
                        List.of(LearningSessionStatus.IN_PROGRESS, LearningSessionStatus.COMPLETED));

        if (activeSessionExists) {
            throw new SessionAlreadyInProgressException("Session already in progress");
        }

        Instant now = Instant.now();

        LearningSession session = new LearningSession();
        session.setUserId(userId);
        session.setResourceId(resourceId);
        session.setSessionType(sessionType);
        session.setStatus(LearningSessionStatus.IN_PROGRESS);
        session.setCurrentStep(1);
        session.setTotalSteps(TOTAL_STEPS);
        session.setStartedAt(now);
        session.setCompletedAt(null);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);

        LearningSession savedSession = learningSessionRepository.save(session);

        return toLearningSessionResponse(savedSession);
    }

    @Transactional
    public DiscardLearningSessionResponse discardSession(
            Long userId, Long resourceId, Long learningSessionId) {
        LearningSession session = learningSessionRepository
                .findByLearningSessionIdAndUserIdAndResourceId(learningSessionId, userId, resourceId)
                .orElseThrow(() -> new LearningSessionNotFoundException("Learning session not found"));

        LearningSessionStatus status = session.getStatus();
        if (status == LearningSessionStatus.RECORD_SAVED || status == LearningSessionStatus.DISCARDED) {
            throw new LearningSessionCannotBeDiscardedException("Learning session cannot be discarded");
        }

        Instant now = Instant.now();
        session.setStatus(LearningSessionStatus.DISCARDED);
        session.setUpdatedAt(now);

        LearningSession savedSession = learningSessionRepository.save(session);

        return toDiscardLearningSessionResponse(savedSession);
    }

    @Transactional
    public SubmitLearningSessionResponseResponse submitResponse(
            Long userId,
            Long resourceId,
            Long learningSessionId,
            SubmitLearningSessionResponseRequest request) {
        LearningSession session = learningSessionRepository
                .findByLearningSessionIdAndUserIdAndResourceId(learningSessionId, userId, resourceId)
                .orElseThrow(() -> new LearningSessionNotFoundException("Learning session not found"));

        if (session.getStatus() != LearningSessionStatus.IN_PROGRESS) {
            throw new LearningSessionCannotAcceptResponseException("Learning session cannot accept response");
        }

        if (session.getCurrentStep() >= session.getTotalSteps()) {
            throw new LearningSessionCannotAcceptResponseException("Learning session cannot accept response");
        }

        Instant now = Instant.now();

        session.setCurrentStep(session.getCurrentStep() + 1);
        session.setUpdatedAt(now);

        LearningSession savedSession = learningSessionRepository.save(session);

        return toSubmitLearningSessionResponseResponse(savedSession);
    }

    private SubmitLearningSessionResponseResponse toSubmitLearningSessionResponseResponse(
            LearningSession session) {
        LearningSessionStepResponse step = new LearningSessionStepResponse();
        step.setCurrentStep(session.getCurrentStep());
        step.setTotalSteps(session.getTotalSteps());
        step.setIsFinalStep(session.getCurrentStep().equals(session.getTotalSteps()));

        LearningSessionNextActionResponse nextAction = new LearningSessionNextActionResponse();
        nextAction.setType(step.getIsFinalStep()
                ? NEXT_ACTION_COMPLETE_SESSION
                : NEXT_ACTION_SUBMIT_RESPONSE);

        SubmitLearningSessionResponseResponse response = new SubmitLearningSessionResponseResponse();
        response.setLearningSessionId(session.getLearningSessionId());
        response.setResourceId(session.getResourceId());
        response.setSessionType(session.getSessionType());
        response.setStatus(session.getStatus());
        response.setAiPrompt(resolveResponseAiPrompt(session.getSessionType(), session.getCurrentStep()));
        response.setStep(step);
        response.setNextAction(nextAction);
        response.setUpdatedAt(session.getUpdatedAt());
        return response;
    }

    private DiscardLearningSessionResponse toDiscardLearningSessionResponse(LearningSession session) {
        DiscardLearningSessionResponse response = new DiscardLearningSessionResponse();
        response.setLearningSessionId(session.getLearningSessionId());
        response.setResourceId(session.getResourceId());
        response.setSessionType(session.getSessionType());
        response.setStatus(session.getStatus());
        response.setUpdatedAt(session.getUpdatedAt());
        return response;
    }

    private LearningSessionResponse toLearningSessionResponse(LearningSession session) {
        LearningSessionStepResponse step = new LearningSessionStepResponse();
        step.setCurrentStep(session.getCurrentStep());
        step.setTotalSteps(session.getTotalSteps());
        step.setIsFinalStep(session.getCurrentStep().equals(session.getTotalSteps()));

        LearningSessionNextActionResponse nextAction = new LearningSessionNextActionResponse();
        nextAction.setType(NEXT_ACTION_SUBMIT_RESPONSE);

        LearningSessionResponse response = new LearningSessionResponse();
        response.setLearningSessionId(session.getLearningSessionId());
        response.setResourceId(session.getResourceId());
        response.setSessionType(session.getSessionType());
        response.setStatus(session.getStatus());
        response.setAiPrompt(resolveResponseAiPrompt(session.getSessionType(), session.getCurrentStep()));
        response.setStep(step);
        response.setNextAction(nextAction);
        response.setStartedAt(session.getStartedAt());
        return response;
    }

    private String resolveResponseAiPrompt(LearningSessionType sessionType, int currentStep) {
        return switch (sessionType) {
            case IMMEDIATE_REFLECTION -> switch (currentStep) {
                case 1 -> IMMEDIATE_REFLECTION_PROMPT_STEP_1;
                case 2 -> IMMEDIATE_REFLECTION_PROMPT_STEP_2;
                case 3 -> IMMEDIATE_REFLECTION_PROMPT_STEP_3;
                default -> throw new LearningSessionCannotAcceptResponseException(
                        "Learning session cannot accept response");
            };
            case DELAYED_RECALL -> switch (currentStep) {
                case 1 -> DELAYED_RECALL_PROMPT_STEP_1;
                case 2 -> DELAYED_RECALL_PROMPT_STEP_2;
                case 3 -> DELAYED_RECALL_PROMPT_STEP_3;
                default -> throw new LearningSessionCannotAcceptResponseException(
                        "Learning session cannot accept response");
            };
        };
    }
}
