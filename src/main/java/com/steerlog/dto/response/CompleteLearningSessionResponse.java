package com.steerlog.dto.response;

import com.steerlog.entity.LearningSessionStatus;
import com.steerlog.entity.LearningSessionType;

import java.time.Instant;

public class CompleteLearningSessionResponse {

    private Long learningSessionId;
    private Long resourceId;
    private LearningSessionType sessionType;
    private LearningSessionStatus status;
    private LearningSessionResultDraftResponse resultDraft;
    private LearningSessionNextActionResponse nextAction;
    private Instant completedAt;

    public CompleteLearningSessionResponse() {
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

    public LearningSessionResultDraftResponse getResultDraft() {
        return resultDraft;
    }

    public void setResultDraft(LearningSessionResultDraftResponse resultDraft) {
        this.resultDraft = resultDraft;
    }

    public LearningSessionNextActionResponse getNextAction() {
        return nextAction;
    }

    public void setNextAction(LearningSessionNextActionResponse nextAction) {
        this.nextAction = nextAction;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
