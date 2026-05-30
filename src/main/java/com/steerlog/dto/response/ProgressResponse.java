package com.steerlog.dto.response;

import com.steerlog.entity.ProgressStatus;

import java.time.Instant;

public class ProgressResponse {

    private Long progressId;
    private ProgressStatus status;
    private Integer currentLevel;
    private Long currentSectionId;
    private Instant startedAt;
    private Instant completedAt;
    private Instant archivedAt;
    private String archiveReason;
    private Instant initialStudiedAt;
    private Instant lastStudiedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public ProgressResponse() {
    }

    public Long getProgressId() {
        return progressId;
    }

    public void setProgressId(Long progressId) {
        this.progressId = progressId;
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

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public String getArchiveReason() {
        return archiveReason;
    }

    public void setArchiveReason(String archiveReason) {
        this.archiveReason = archiveReason;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
