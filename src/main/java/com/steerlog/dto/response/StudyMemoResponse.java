package com.steerlog.dto.response;

import com.steerlog.entity.StudyMemoType;

import java.time.Instant;

public class StudyMemoResponse {

    private Long studyMemoId;
    private Long resourceId;
    private Long resourceSectionId;
    private StudyMemoType memoType;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;

    public StudyMemoResponse() {
    }

    public Long getStudyMemoId() {
        return studyMemoId;
    }

    public void setStudyMemoId(Long studyMemoId) {
        this.studyMemoId = studyMemoId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
