package com.steerlog.dto.response;

import java.util.List;

public class LearningSessionResultDraftResponse {

    private String summary;
    private List<String> conceptTags;
    private String weakPointSummary;
    private String nextAction;
    private String aiAssessment;
    private String generationBasis;

    public LearningSessionResultDraftResponse() {
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

    public String getAiAssessment() {
        return aiAssessment;
    }

    public void setAiAssessment(String aiAssessment) {
        this.aiAssessment = aiAssessment;
    }

    public String getGenerationBasis() {
        return generationBasis;
    }

    public void setGenerationBasis(String generationBasis) {
        this.generationBasis = generationBasis;
    }
}
