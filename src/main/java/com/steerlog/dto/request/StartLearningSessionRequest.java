package com.steerlog.dto.request;

import com.steerlog.entity.LearningSessionType;
import jakarta.validation.constraints.NotNull;

public class StartLearningSessionRequest {

    @NotNull
    private LearningSessionType sessionType;

    public StartLearningSessionRequest() {
    }

    public LearningSessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(LearningSessionType sessionType) {
        this.sessionType = sessionType;
    }
}
