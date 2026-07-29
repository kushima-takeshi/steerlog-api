package com.steerlog.dto.request;

import com.steerlog.entity.ProgressStatus;
import jakarta.validation.constraints.Size;

public class UpdateProgressRequest {

    private ProgressStatus status;

    private Long currentSectionId;

    @Size(max = 500)
    private String archiveReason;

    public UpdateProgressRequest() {
    }

    public ProgressStatus getStatus() {
        return status;
    }

    public void setStatus(ProgressStatus status) {
        this.status = status;
    }

    public Long getCurrentSectionId() {
        return currentSectionId;
    }

    public void setCurrentSectionId(Long currentSectionId) {
        this.currentSectionId = currentSectionId;
    }

    public String getArchiveReason() {
        return archiveReason;
    }

    public void setArchiveReason(String archiveReason) {
        this.archiveReason = archiveReason;
    }
}
