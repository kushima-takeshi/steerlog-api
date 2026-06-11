package com.steerlog.dto.request;

import com.steerlog.entity.LearningSessionAiAssessment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class SaveLearningSessionRecordRequest {

    @NotBlank
    @Size(min = 1, max = 1000)
    private String summary;

    @Size(max = 10)
    private List<@NotBlank @Size(min = 1, max = 50) String> conceptTags;

    @Size(max = 1000)
    private String weakPointSummary;

    @Size(max = 1000)
    private String nextAction;

    @NotNull
    private LearningSessionAiAssessment aiAssessment;

    public SaveLearningSessionRecordRequest() {
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
}
