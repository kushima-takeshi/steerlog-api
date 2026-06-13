package com.steerlog.dto.response;

import java.util.List;

public class ResourceDetailsResponse {

    private ResourceSummaryResponse resource;
    private ProgressSummaryResponse progress;
    private List<ResourceSectionWithStudyStatusResponse> sections;
    private List<StudyMemoSummaryResponse> memos;
    private List<LevelHistorySummaryResponse> levelHistories;
    private List<LearningSessionRecordSummaryResponse> learningSessionRecords;

    public ResourceDetailsResponse() {
    }

    public ResourceSummaryResponse getResource() {
        return resource;
    }

    public void setResource(ResourceSummaryResponse resource) {
        this.resource = resource;
    }

    public ProgressSummaryResponse getProgress() {
        return progress;
    }

    public void setProgress(ProgressSummaryResponse progress) {
        this.progress = progress;
    }

    public List<ResourceSectionWithStudyStatusResponse> getSections() {
        return sections;
    }

    public void setSections(List<ResourceSectionWithStudyStatusResponse> sections) {
        this.sections = sections;
    }

    public List<StudyMemoSummaryResponse> getMemos() {
        return memos;
    }

    public void setMemos(List<StudyMemoSummaryResponse> memos) {
        this.memos = memos;
    }

    public List<LevelHistorySummaryResponse> getLevelHistories() {
        return levelHistories;
    }

    public void setLevelHistories(List<LevelHistorySummaryResponse> levelHistories) {
        this.levelHistories = levelHistories;
    }

    public List<LearningSessionRecordSummaryResponse> getLearningSessionRecords() {
        return learningSessionRecords;
    }

    public void setLearningSessionRecords(List<LearningSessionRecordSummaryResponse> learningSessionRecords) {
        this.learningSessionRecords = learningSessionRecords;
    }
}
