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
@Table(name = "learning_session_records")
public class LearningSessionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_session_record_id")
    private Long learningSessionRecordId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "learning_session_id", nullable = false)
    private Long learningSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 50)
    private LearningSessionType sessionType;

    @Column(name = "summary", nullable = false)
    private String summary;

    @Column(name = "concept_tags")
    private String conceptTags;

    @Column(name = "weak_point_summary")
    private String weakPointSummary;

    @Column(name = "next_action")
    private String nextAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_assessment", nullable = false, length = 50)
    private LearningSessionAiAssessment aiAssessment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LearningSessionRecord() {
    }

    public Long getLearningSessionRecordId() {
        return learningSessionRecordId;
    }

    public void setLearningSessionRecordId(Long learningSessionRecordId) {
        this.learningSessionRecordId = learningSessionRecordId;
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

    public Long getLearningSessionId() {
        return learningSessionId;
    }

    public void setLearningSessionId(Long learningSessionId) {
        this.learningSessionId = learningSessionId;
    }

    public LearningSessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(LearningSessionType sessionType) {
        this.sessionType = sessionType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getConceptTags() {
        return conceptTags;
    }

    public void setConceptTags(String conceptTags) {
        this.conceptTags = conceptTags;
    }

    public String getWeakPointSummary() {
        return weakPointSummary;
    }

    public void setWeakPointSummary(String weakPointSummary) {
        this.weakPointSummary = weakPointSummary;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    public LearningSessionAiAssessment getAiAssessment() {
        return aiAssessment;
    }

    public void setAiAssessment(LearningSessionAiAssessment aiAssessment) {
        this.aiAssessment = aiAssessment;
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
