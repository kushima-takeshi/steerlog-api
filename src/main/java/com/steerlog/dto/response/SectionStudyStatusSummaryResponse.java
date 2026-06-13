package com.steerlog.dto.response;

import java.time.Instant;

public class SectionStudyStatusSummaryResponse {

    private Instant studiedAt;

    public SectionStudyStatusSummaryResponse() {
    }

    public Instant getStudiedAt() {
        return studiedAt;
    }

    public void setStudiedAt(Instant studiedAt) {
        this.studiedAt = studiedAt;
    }
}
