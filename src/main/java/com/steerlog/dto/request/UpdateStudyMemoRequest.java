package com.steerlog.dto.request;

import com.steerlog.entity.StudyMemoType;
import jakarta.validation.constraints.Size;

public class UpdateStudyMemoRequest {

    private StudyMemoType memoType;

    @Size(min = 1, max = 500)
    private String content;

    public UpdateStudyMemoRequest() {
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
