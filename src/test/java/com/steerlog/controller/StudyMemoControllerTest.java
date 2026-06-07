package com.steerlog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steerlog.dto.request.CreateStudyMemoRequest;
import com.steerlog.dto.response.StudyMemoResponse;
import com.steerlog.entity.StudyMemoType;
import com.steerlog.exception.GlobalExceptionHandler;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.service.StudyMemoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyMemoController.class)
@Import(GlobalExceptionHandler.class)
class StudyMemoControllerTest {

    private static final Long TEMP_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudyMemoService studyMemoService;

    @Test
    void createMemo_shouldReturn201() throws Exception {
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        CreateStudyMemoRequest request = new CreateStudyMemoRequest();
        request.setContent("HTTPの基本を理解した");
        request.setMemoType(StudyMemoType.LEARNED);

        StudyMemoResponse response = new StudyMemoResponse();
        response.setStudyMemoId(500L);
        response.setResourceId(resourceId);
        response.setMemoType(StudyMemoType.LEARNED);
        response.setContent("HTTPの基本を理解した");
        response.setCreatedAt(now);
        response.setUpdatedAt(now);

        when(studyMemoService.createMemo(eq(TEMP_USER_ID), eq(resourceId), any(CreateStudyMemoRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/resources/{resourceId}/memos", resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studyMemoId").value(500))
                .andExpect(jsonPath("$.resourceId").value(10))
                .andExpect(jsonPath("$.memoType").value("LEARNED"))
                .andExpect(jsonPath("$.content").value("HTTPの基本を理解した"));

        verify(studyMemoService).createMemo(eq(TEMP_USER_ID), eq(resourceId), any(CreateStudyMemoRequest.class));
    }

    @Test
    void getMemos_shouldReturn200WithMemos() throws Exception {
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        StudyMemoResponse response = new StudyMemoResponse();
        response.setStudyMemoId(500L);
        response.setResourceId(resourceId);
        response.setMemoType(StudyMemoType.LEARNED);
        response.setContent("HTTPの基本を理解した");
        response.setCreatedAt(now);
        response.setUpdatedAt(now);

        when(studyMemoService.getMemos(TEMP_USER_ID, resourceId))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/resources/{resourceId}/memos", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studyMemoId").value(500))
                .andExpect(jsonPath("$[0].memoType").value("LEARNED"))
                .andExpect(jsonPath("$[0].content").value("HTTPの基本を理解した"));

        verify(studyMemoService).getMemos(TEMP_USER_ID, resourceId);
    }

    @Test
    void getMemos_shouldReturn200WithEmptyList() throws Exception {
        Long resourceId = 10L;

        when(studyMemoService.getMemos(TEMP_USER_ID, resourceId))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/resources/{resourceId}/memos", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(studyMemoService).getMemos(TEMP_USER_ID, resourceId);
    }

    @Test
    void createMemo_shouldReturn404WhenResourceNotFound() throws Exception {
        Long resourceId = 10L;

        CreateStudyMemoRequest request = new CreateStudyMemoRequest();
        request.setContent("メモ内容");

        when(studyMemoService.createMemo(eq(TEMP_USER_ID), eq(resourceId), any(CreateStudyMemoRequest.class)))
                .thenThrow(new ResourceNotFoundException("Resource not found"));

        mockMvc.perform(post("/resources/{resourceId}/memos", resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));

        verify(studyMemoService).createMemo(eq(TEMP_USER_ID), eq(resourceId), any(CreateStudyMemoRequest.class));
    }

    @Test
    void deleteMemo_shouldReturn204() throws Exception {
        Long resourceId = 10L;
        Long memoId = 500L;

        doNothing().when(studyMemoService).deleteMemo(TEMP_USER_ID, resourceId, memoId);

        mockMvc.perform(delete("/resources/{resourceId}/memos/{memoId}", resourceId, memoId))
                .andExpect(status().isNoContent());

        verify(studyMemoService).deleteMemo(TEMP_USER_ID, resourceId, memoId);
    }

    @Test
    void deleteMemo_shouldReturn404WhenMemoNotFound() throws Exception {
        Long resourceId = 10L;
        Long memoId = 500L;

        doThrow(new ResourceNotFoundException("Resource not found"))
                .when(studyMemoService).deleteMemo(TEMP_USER_ID, resourceId, memoId);

        mockMvc.perform(delete("/resources/{resourceId}/memos/{memoId}", resourceId, memoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));

        verify(studyMemoService).deleteMemo(TEMP_USER_ID, resourceId, memoId);
    }
}
