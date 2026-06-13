package com.steerlog.dto.response;

import com.steerlog.entity.StudyMemoType;

import java.time.Instant;

public class StudyMemoSummaryResponse {

    private Long studyMemoId;
    private Long resourceSectionId;
    private StudyMemoType memoType;
    private String content;
    private Instant createdAt;

    public StudyMemoSummaryResponse() {
    }

    public Long getStudyMemoId() {
        return studyMemoId;
    }

    public void setStudyMemoId(Long studyMemoId) {
        this.studyMemoId = studyMemoId;
    }

    public Long getResourceSectionId() {
        return resourceSectionId;
    }

    public void setResourceSectionId(Long resourceSectionId) {
        this.resourceSectionId = resourceSectionId;
    }

    public StudyMemoType getMemoType() {
        return memoType;
    }

    public void setMemoType(StudyMemoType memoType) {
        this.memoType = memoType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
