package com.steerlog.controller;

import com.steerlog.dto.response.LevelHistorySummaryResponse;
import com.steerlog.dto.response.LearningSessionRecordSummaryResponse;
import com.steerlog.dto.response.ProgressSummaryResponse;
import com.steerlog.dto.response.ResourceDetailsResponse;
import com.steerlog.dto.response.ResourceSectionWithStudyStatusResponse;
import com.steerlog.dto.response.ResourceSummaryResponse;
import com.steerlog.dto.response.SectionStudyStatusSummaryResponse;
import com.steerlog.dto.response.StudyMemoSummaryResponse;
import com.steerlog.entity.LearningSessionAiAssessment;
import com.steerlog.entity.LearningSessionType;
import com.steerlog.entity.LevelHistoryReasonCode;
import com.steerlog.entity.LevelHistorySourceType;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.entity.ResourceType;
import com.steerlog.entity.StudyMemoType;
import com.steerlog.exception.GlobalExceptionHandler;
import com.steerlog.exception.ProgressNotFoundException;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.service.ResourceDetailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourceDetailController.class)
@Import(GlobalExceptionHandler.class)
class ResourceDetailControllerTest {

    private static final Long TEMP_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceDetailService resourceDetailService;

    @Test
    void getResourceDetails_shouldReturn200WithAggregatedDetails() throws Exception {
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-11T10:00:00Z");

        ResourceSummaryResponse resource = new ResourceSummaryResponse();
        resource.setResourceId(resourceId);
        resource.setResourceType(ResourceType.BOOK);
        resource.setTitle("Webを支える技術");
        resource.setAuthor("山本陽平");

        ProgressSummaryResponse progress = new ProgressSummaryResponse();
        progress.setStatus(ProgressStatus.IN_PROGRESS);
        progress.setCurrentLevel(3);
        progress.setCurrentSectionId(100L);
        progress.setInitialStudiedAt(now);
        progress.setLastStudiedAt(now);

        SectionStudyStatusSummaryResponse studyStatus = new SectionStudyStatusSummaryResponse();
        studyStatus.setStudiedAt(now);

        ResourceSectionWithStudyStatusResponse section = new ResourceSectionWithStudyStatusResponse();
        section.setResourceSectionId(100L);
        section.setTitle("第1章 Webとは何か");
        section.setSectionOrder(1);
        section.setStudyStatus(studyStatus);

        StudyMemoSummaryResponse memo = new StudyMemoSummaryResponse();
        memo.setStudyMemoId(300L);
        memo.setResourceSectionId(100L);
        memo.setMemoType(StudyMemoType.QUESTION);
        memo.setContent("PUTとPATCHの違いがまだ曖昧");
        memo.setCreatedAt(now);

        LevelHistorySummaryResponse levelHistory = new LevelHistorySummaryResponse();
        levelHistory.setLevel(1);
        levelHistory.setSourceType(LevelHistorySourceType.INITIAL_STUDY_COMPLETION);
        levelHistory.setSourceId(null);
        levelHistory.setReasonCode(LevelHistoryReasonCode.INITIAL_STUDY_COMPLETED);
        levelHistory.setCreatedAt(now);

        LearningSessionRecordSummaryResponse record = new LearningSessionRecordSummaryResponse();
        record.setLearningSessionRecordId(400L);
        record.setSessionType(LearningSessionType.IMMEDIATE_REFLECTION);
        record.setSummary("REST APIの基本を説明できた");
        record.setConceptTags(List.of("REST", "HTTP"));
        record.setWeakPointSummary("PATCHの使い分けが曖昧");
        record.setNextAction("Progress更新APIでPATCH設計を整理する");
        record.setAiAssessment(LearningSessionAiAssessment.NEEDS_REVIEW);
        record.setCreatedAt(now);

        ResourceDetailsResponse response = new ResourceDetailsResponse();
        response.setResource(resource);
        response.setProgress(progress);
        response.setSections(List.of(section));
        response.setMemos(List.of(memo));
        response.setLevelHistories(List.of(levelHistory));
        response.setLearningSessionRecords(List.of(record));

        when(resourceDetailService.getResourceDetails(TEMP_USER_ID, resourceId)).thenReturn(response);

        mockMvc.perform(get("/resources/{resourceId}/details", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.resourceId").value(10))
                .andExpect(jsonPath("$.resource.title").value("Webを支える技術"))
                .andExpect(jsonPath("$.progress.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.progress.currentLevel").value(3))
                .andExpect(jsonPath("$.sections[0].resourceSectionId").value(100))
                .andExpect(jsonPath("$.sections[0].studyStatus.studiedAt").value("2026-06-11T10:00:00Z"))
                .andExpect(jsonPath("$.memos[0].studyMemoId").value(300))
                .andExpect(jsonPath("$.levelHistories[0].level").value(1))
                .andExpect(jsonPath("$.levelHistories[0].reasonCode").value("INITIAL_STUDY_COMPLETED"))
                .andExpect(jsonPath("$.learningSessionRecords[0].learningSessionRecordId").value(400))
                .andExpect(jsonPath("$.learningSessionRecords[0].conceptTags[0]").value("REST"));

        verify(resourceDetailService).getResourceDetails(TEMP_USER_ID, resourceId);
    }

    @Test
    void getResourceDetails_shouldReturn404WhenResourceNotFound() throws Exception {
        Long resourceId = 10L;

        when(resourceDetailService.getResourceDetails(TEMP_USER_ID, resourceId))
                .thenThrow(new ResourceNotFoundException("Resource not found"));

        mockMvc.perform(get("/resources/{resourceId}/details", resourceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));

        verify(resourceDetailService).getResourceDetails(TEMP_USER_ID, resourceId);
    }

    @Test
    void getResourceDetails_shouldReturn404WhenProgressNotFound() throws Exception {
        Long resourceId = 10L;

        when(resourceDetailService.getResourceDetails(TEMP_USER_ID, resourceId))
                .thenThrow(new ProgressNotFoundException("Progress not found"));

        mockMvc.perform(get("/resources/{resourceId}/details", resourceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROGRESS_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Progress not found"));

        verify(resourceDetailService).getResourceDetails(TEMP_USER_ID, resourceId);
    }
}
