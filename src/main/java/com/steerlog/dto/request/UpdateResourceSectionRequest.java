package com.steerlog.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateResourceSectionRequest {

    @Size(max = 255)
    @Pattern(regexp = "(?U)^(?!\\s*$).+", message = "title must not be blank")
    private String title;

    @Min(1)
    private Integer sectionOrder;

    public UpdateResourceSectionRequest() {
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
}
