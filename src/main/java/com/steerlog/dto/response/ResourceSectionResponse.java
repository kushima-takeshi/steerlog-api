package com.steerlog.dto.response;

import java.time.Instant;

public class ResourceSectionResponse {

    private Long resourceSectionId;
    private Long resourceId;
    private String title;
    private Integer sectionOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public ResourceSectionResponse() {
    }

    public Long getResourceSectionId() {
        return resourceSectionId;
    }

    public void setResourceSectionId(Long resourceSectionId) {
        this.resourceSectionId = resourceSectionId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getSectionOrder() {
        return sectionOrder;
    }

    public void setSectionOrder(Integer sectionOrder) {
        this.sectionOrder = sectionOrder;
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
