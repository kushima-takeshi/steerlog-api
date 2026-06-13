package com.steerlog.dto.response;

public class ResourceSectionWithStudyStatusResponse {

    private Long resourceSectionId;
    private String title;
    private Integer sectionOrder;
    private SectionStudyStatusSummaryResponse studyStatus;

    public ResourceSectionWithStudyStatusResponse() {
    }

    public Long getResourceSectionId() {
        return resourceSectionId;
    }

    public void setResourceSectionId(Long resourceSectionId) {
        this.resourceSectionId = resourceSectionId;
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

    public SectionStudyStatusSummaryResponse getStudyStatus() {
        return studyStatus;
    }

    public void setStudyStatus(SectionStudyStatusSummaryResponse studyStatus) {
        this.studyStatus = studyStatus;
    }
}
