package com.steerlog.dto.request;

import jakarta.validation.constraints.Size;

public class UpdateResourceRequest {

    @Size(min = 1, max = 255)
    private String title;

    @Size(max = 255)
    private String author;

    private String sourceUrl;

    private String description;

    public UpdateResourceRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
