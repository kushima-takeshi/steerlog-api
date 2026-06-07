package com.steerlog.dto.response;

import java.time.Instant;

public class SectionStudyStatusResponse {

    private Long sectionStudyStatusId;
    private Long resourceId;
    private Long resourceSectionId;
    private Instant studiedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public SectionStudyStatusResponse() {
    }

    public Long getSectionStudyStatusId() {
        return sectionStudyStatusId;
    }

    public void setSectionStudyStatusId(Long sectionStudyStatusId) {
        this.sectionStudyStatusId = sectionStudyStatusId;
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

    public Instant getStudiedAt() {
        return studiedAt;
    }

    public void setStudiedAt(Instant studiedAt) {
        this.studiedAt = studiedAt;
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
