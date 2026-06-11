package com.steerlog.dto.response;

import com.steerlog.entity.LearningSessionAiAssessment;
import com.steerlog.entity.LearningSessionType;

import java.time.Instant;
import java.util.List;

public class LearningSessionRecordResponse {

    private Long learningSessionRecordId;
    private Long resourceId;
    private Long learningSessionId;
    private LearningSessionType sessionType;
    private String summary;
    private List<String> conceptTags;
    private String weakPointSummary;
    private String nextAction;
    private LearningSessionAiAssessment aiAssessment;
    private Instant createdAt;

    public LearningSessionRecordResponse() {
    }

    public Long getLearningSessionRecordId() {
        return learningSessionRecordId;
    }

    public void setLearningSessionRecordId(Long learningSessionRecordId) {
        this.learningSessionRecordId = learningSessionRecordId;
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

    public List<String> getConceptTags() {
        return conceptTags;
    }

    public void setConceptTags(List<String> conceptTags) {
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
}
