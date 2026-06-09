package com.steerlog.dto.response;

import com.steerlog.entity.LearningSessionStatus;
import com.steerlog.entity.LearningSessionType;

import java.time.Instant;

public class DiscardLearningSessionResponse {

    private Long learningSessionId;
    private Long resourceId;
    private LearningSessionType sessionType;
    private LearningSessionStatus status;
    private Instant updatedAt;

    public DiscardLearningSessionResponse() {
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
