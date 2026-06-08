package com.steerlog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "learning_sessions")
public class LearningSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_session_id")
    private Long learningSessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 50)
    private LearningSessionType sessionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private LearningSessionStatus status;

    @Column(name = "current_step", nullable = false)
    private Integer currentStep;

    @Column(name = "total_steps", nullable = false)
    private Integer totalSteps;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LearningSession() {
    }

    public Long getLearningSessionId() {
        return learningSessionId;
    }

    public void setLearningSessionId(Long learningSessionId) {
        this.learningSessionId = learningSessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Integer getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = currentStep;
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
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
