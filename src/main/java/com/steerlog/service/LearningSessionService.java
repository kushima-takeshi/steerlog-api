package com.steerlog.service;

import com.steerlog.dto.request.SaveLearningSessionRecordRequest;
import com.steerlog.dto.request.StartLearningSessionRequest;
import com.steerlog.dto.request.SubmitLearningSessionResponseRequest;
import com.steerlog.dto.response.CompleteLearningSessionResponse;
import com.steerlog.dto.response.DiscardLearningSessionResponse;
import com.steerlog.dto.response.LearningSessionResultDraftResponse;
import com.steerlog.dto.response.SubmitLearningSessionResponseResponse;
import com.steerlog.dto.response.LearningSessionNextActionResponse;
import com.steerlog.dto.response.LearningSessionRecordResponse;
import com.steerlog.dto.response.LearningSessionResponse;
import com.steerlog.dto.response.LearningSessionStepResponse;
import com.steerlog.entity.LearningSession;
import com.steerlog.entity.LearningSessionAiAssessment;
import com.steerlog.entity.LearningSessionRecord;
import com.steerlog.entity.LearningSessionStatus;
import com.steerlog.entity.LearningSessionType;
import com.steerlog.entity.Progress;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LearningSessionService {

    private static final int TOTAL_STEPS = 3;
    private static final String NEXT_ACTION_SUBMIT_RESPONSE = "SUBMIT_RESPONSE";
    private static final String NEXT_ACTION_COMPLETE_SESSION = "COMPLETE_SESSION";
    private static final String NEXT_ACTION_SAVE_RECORD = "SAVE_RECORD";

    private static final String RESULT_DRAFT_GENERATION_BASIS =
            "MVPでは回答ログを保存しないため、固定テンプレートでresultDraftを生成しています。";
    private static final String RESULT_DRAFT_AI_ASSESSMENT = "PASSED";

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
    private final LearningSessionRecordRepository learningSessionRecordRepository;

    public LearningSessionService(
            ResourceRepository resourceRepository,
            ProgressRepository progressRepository,
            LearningSessionRepository learningSessionRepository,
            LearningSessionRecordRepository learningSessionRecordRepository) {
        this.resourceRepository = resourceRepository;
        this.progressRepository = progressRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.learningSessionRecordRepository = learningSessionRecordRepository;
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

    @Transactional
    public CompleteLearningSessionResponse completeSession(
            Long userId, Long resourceId, Long learningSessionId) {
        LearningSession session = learningSessionRepository
                .findByLearningSessionIdAndUserIdAndResourceId(learningSessionId, userId, resourceId)
                .orElseThrow(() -> new LearningSessionNotFoundException("Learning session not found"));

        if (session.getStatus() != LearningSessionStatus.IN_PROGRESS) {
            throw new LearningSessionCannotBeCompletedException("Learning session cannot be completed");
        }

        if (!session.getCurrentStep().equals(session.getTotalSteps())) {
            throw new LearningSessionCannotBeCompletedException("Learning session cannot be completed");
        }

        Instant now = Instant.now();
        session.setStatus(LearningSessionStatus.COMPLETED);
        session.setCompletedAt(now);
        session.setUpdatedAt(now);

        LearningSession savedSession = learningSessionRepository.save(session);

        return toCompleteLearningSessionResponse(savedSession);
    }

    @Transactional
    public LearningSessionRecordResponse saveRecord(
            Long userId,
            Long resourceId,
            Long learningSessionId,
            SaveLearningSessionRecordRequest request) {
        LearningSession session = learningSessionRepository
                .findByLearningSessionIdAndUserIdAndResourceId(learningSessionId, userId, resourceId)
                .orElseThrow(() -> new LearningSessionNotFoundException("Learning session not found"));

        if (session.getStatus() != LearningSessionStatus.COMPLETED) {
            throw new LearningSessionRecordCannotBeSavedException("Learning session record cannot be saved");
        }

        if (request.getAiAssessment() == LearningSessionAiAssessment.OFF_TOPIC) {
            throw new LearningSessionRecordCannotBeSavedException("Learning session record cannot be saved");
        }

        if (learningSessionRecordRepository.existsByLearningSessionIdAndUserIdAndResourceId(
                learningSessionId, userId, resourceId)) {
            throw new LearningSessionRecordCannotBeSavedException("Learning session record cannot be saved");
        }

        Instant now = Instant.now();

        LearningSessionRecord record = new LearningSessionRecord();
        record.setUserId(userId);
        record.setResourceId(resourceId);
        record.setLearningSessionId(learningSessionId);
        record.setSessionType(session.getSessionType());
        record.setSummary(request.getSummary());
        record.setConceptTags(formatConceptTags(request.getConceptTags()));
        record.setWeakPointSummary(request.getWeakPointSummary());
        record.setNextAction(request.getNextAction());
        record.setAiAssessment(request.getAiAssessment());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);

        LearningSessionRecord savedRecord = learningSessionRecordRepository.save(record);

        session.setStatus(LearningSessionStatus.RECORD_SAVED);
        session.setUpdatedAt(now);
        learningSessionRepository.save(session);

        return toLearningSessionRecordResponse(savedRecord);
    }

    private String formatConceptTags(List<String> conceptTags) {
        if (conceptTags == null || conceptTags.isEmpty()) {
            return null;
        }
        return String.join(",", conceptTags);
    }

    private List<String> parseConceptTags(String conceptTags) {
        if (conceptTags == null || conceptTags.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(conceptTags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }

    private LearningSessionRecordResponse toLearningSessionRecordResponse(LearningSessionRecord record) {
        LearningSessionRecordResponse response = new LearningSessionRecordResponse();
        response.setLearningSessionRecordId(record.getLearningSessionRecordId());
        response.setResourceId(record.getResourceId());
        response.setLearningSessionId(record.getLearningSessionId());
        response.setSessionType(record.getSessionType());
        response.setSummary(record.getSummary());
        response.setConceptTags(parseConceptTags(record.getConceptTags()));
        response.setWeakPointSummary(record.getWeakPointSummary());
        response.setNextAction(record.getNextAction());
        response.setAiAssessment(record.getAiAssessment());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }

    private CompleteLearningSessionResponse toCompleteLearningSessionResponse(LearningSession session) {
        LearningSessionNextActionResponse nextAction = new LearningSessionNextActionResponse();
        nextAction.setType(NEXT_ACTION_SAVE_RECORD);

        CompleteLearningSessionResponse response = new CompleteLearningSessionResponse();
        response.setLearningSessionId(session.getLearningSessionId());
        response.setResourceId(session.getResourceId());
        response.setSessionType(session.getSessionType());
        response.setStatus(session.getStatus());
        response.setResultDraft(buildResultDraft(session.getSessionType()));
        response.setNextAction(nextAction);
        response.setCompletedAt(session.getCompletedAt());
        return response;
    }

    private LearningSessionResultDraftResponse buildResultDraft(LearningSessionType sessionType) {
        LearningSessionResultDraftResponse resultDraft = new LearningSessionResultDraftResponse();
        resultDraft.setAiAssessment(RESULT_DRAFT_AI_ASSESSMENT);
        resultDraft.setGenerationBasis(RESULT_DRAFT_GENERATION_BASIS);

        switch (sessionType) {
            case IMMEDIATE_REFLECTION -> {
                resultDraft.setSummary("今回の振り返り内容をもとに、学習内容の要点を整理しました。");
                resultDraft.setConceptTags(List.of("reflection", "understanding", "review"));
                resultDraft.setWeakPointSummary("まだ曖昧な点は、次回の復習で確認してください。");
                resultDraft.setNextAction(
                        "今回整理した内容をもとに、重要な概念をもう一度説明できるか確認してください。");
            }
            case DELAYED_RECALL -> {
                resultDraft.setSummary("今回の想起内容をもとに、記憶に残っている内容を整理しました。");
                resultDraft.setConceptTags(List.of("recall", "retention", "review"));
                resultDraft.setWeakPointSummary(
                        "思い出せなかった点や曖昧な点を、次回の学習対象として確認してください。");
                resultDraft.setNextAction(
                        "曖昧だった内容をResourceに戻って確認し、再度説明できるか試してください。");
            }
        }

        return resultDraft;
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
