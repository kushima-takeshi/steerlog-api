package com.steerlog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steerlog.dto.request.UpdateSectionStudyStatusRequest;
import com.steerlog.dto.response.SectionStudyStatusResponse;
import com.steerlog.exception.GlobalExceptionHandler;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.service.SectionStudyStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SectionStudyStatusController.class)
@Import(GlobalExceptionHandler.class)
class SectionStudyStatusControllerTest {

    private static final Long TEMP_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SectionStudyStatusService sectionStudyStatusService;

    @Test
    void updateStudyStatus_shouldReturn200() throws Exception {
        Long resourceId = 10L;
        Long sectionId = 100L;
        Instant studiedAt = Instant.parse("2026-06-05T10:00:00Z");
        Instant now = Instant.parse("2026-06-05T12:00:00Z");

        UpdateSectionStudyStatusRequest request = new UpdateSectionStudyStatusRequest();
        request.setStudiedAt(studiedAt);

        SectionStudyStatusResponse response = new SectionStudyStatusResponse();
        response.setSectionStudyStatusId(200L);
        response.setResourceId(resourceId);
        response.setResourceSectionId(sectionId);
        response.setStudiedAt(studiedAt);
        response.setCreatedAt(now);
        response.setUpdatedAt(now);

        when(sectionStudyStatusService.updateStudyStatus(
                eq(TEMP_USER_ID), eq(resourceId), eq(sectionId), any(UpdateSectionStudyStatusRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/resources/{resourceId}/sections/{sectionId}/study-status", resourceId, sectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionStudyStatusId").value(200))
                .andExpect(jsonPath("$.resourceId").value(10))
                .andExpect(jsonPath("$.resourceSectionId").value(100))
                .andExpect(jsonPath("$.studiedAt").value("2026-06-05T10:00:00Z"));

        verify(sectionStudyStatusService).updateStudyStatus(
                eq(TEMP_USER_ID), eq(resourceId), eq(sectionId), any(UpdateSectionStudyStatusRequest.class));
    }

    @Test
    void updateStudyStatus_shouldReturn404WhenResourceNotFound() throws Exception {
        Long resourceId = 10L;
        Long sectionId = 100L;

        UpdateSectionStudyStatusRequest request = new UpdateSectionStudyStatusRequest();
        request.setStudiedAt(Instant.parse("2026-06-05T10:00:00Z"));

        when(sectionStudyStatusService.updateStudyStatus(
                eq(TEMP_USER_ID), eq(resourceId), eq(sectionId), any(UpdateSectionStudyStatusRequest.class)))
                .thenThrow(new ResourceNotFoundException("Resource not found"));

        mockMvc.perform(patch("/resources/{resourceId}/sections/{sectionId}/study-status", resourceId, sectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));

        verify(sectionStudyStatusService).updateStudyStatus(
                eq(TEMP_USER_ID), eq(resourceId), eq(sectionId), any(UpdateSectionStudyStatusRequest.class));
    }
}
