package com.steerlog.dto.request;

import com.steerlog.entity.StudyMemoType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateStudyMemoRequest {

    private Long resourceSectionId;

    private StudyMemoType memoType;

    @NotBlank
    @Size(min = 1, max = 500)
    private String content;

    public CreateStudyMemoRequest() {
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
}
