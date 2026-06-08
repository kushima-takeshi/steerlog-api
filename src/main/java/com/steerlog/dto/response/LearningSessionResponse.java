package com.steerlog.dto.response;

import com.steerlog.entity.LearningSessionStatus;
import com.steerlog.entity.LearningSessionType;

import java.time.Instant;

public class LearningSessionResponse {

    private Long learningSessionId;
    private Long resourceId;
    private LearningSessionType sessionType;
    private LearningSessionStatus status;
    private String aiPrompt;
    private LearningSessionStepResponse step;
    private LearningSessionNextActionResponse nextAction;
    private Instant startedAt;

    public LearningSessionResponse() {
    }

    public Long getLearningSessionId() {
        return learningSessionId;
    }

    public void setLearningSessionId(Long learningSessionId) {
        this.learningSessionId = learningSessionId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public LearningSessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(LearningSessionType sessionType) {
        this.sessionType = sessionType;
    }

    public LearningSessionStatus getStatus() {
        return status;
    }

    public void setStatus(LearningSessionStatus status) {
        this.status = status;
    }

    public String getAiPrompt() {
        return aiPrompt;
    }

    public void setAiPrompt(String aiPrompt) {
        this.aiPrompt = aiPrompt;
    }

    public LearningSessionStepResponse getStep() {
        return step;
    }

    public void setStep(LearningSessionStepResponse step) {
        this.step = step;
    }

    public LearningSessionNextActionResponse getNextAction() {
        return nextAction;
    }

    public void setNextAction(LearningSessionNextActionResponse nextAction) {
        this.nextAction = nextAction;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
}
