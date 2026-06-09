package com.steerlog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SubmitLearningSessionResponseRequest {

    @NotBlank
    @Size(min = 1, max = 1000)
    private String responseText;

    public SubmitLearningSessionResponseRequest() {
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }
}
