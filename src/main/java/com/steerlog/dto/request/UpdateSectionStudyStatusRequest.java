package com.steerlog.dto.request;

import java.time.Instant;

public class UpdateSectionStudyStatusRequest {

    private Instant studiedAt;

    public UpdateSectionStudyStatusRequest() {
    }

    public Instant getStudiedAt() {
        return studiedAt;
    }

    public void setStudiedAt(Instant studiedAt) {
        this.studiedAt = studiedAt;
    }
}
