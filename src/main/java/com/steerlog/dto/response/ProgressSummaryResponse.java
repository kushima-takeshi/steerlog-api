package com.steerlog.dto.response;

import com.steerlog.entity.ProgressStatus;

import java.time.Instant;

public class ProgressSummaryResponse {

    private ProgressStatus status;
    private Integer currentLevel;
    private Long currentSectionId;
    private Instant initialStudiedAt;
    private Instant lastStudiedAt;

    public ProgressSummaryResponse() {
    }

    public ProgressStatus getStatus() {
        return status;
    }

    public void setStatus(ProgressStatus status) {
        this.status = status;
    }

    public Integer getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Integer currentLevel) {
        this.currentLevel = currentLevel;
    }

    public Long getCurrentSectionId() {
        return currentSectionId;
    }

    public void setCurrentSectionId(Long currentSectionId) {
        this.currentSectionId = currentSectionId;
    }

    public Instant getInitialStudiedAt() {
        return initialStudiedAt;
    }

    public void setInitialStudiedAt(Instant initialStudiedAt) {
        this.initialStudiedAt = initialStudiedAt;
    }

    public Instant getLastStudiedAt() {
        return lastStudiedAt;
    }

    public void setLastStudiedAt(Instant lastStudiedAt) {
        this.lastStudiedAt = lastStudiedAt;
    }
}
