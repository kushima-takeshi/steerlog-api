package com.steerlog.controller;

import com.steerlog.dto.response.ProgressResponse;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.exception.GlobalExceptionHandler;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.service.ProgressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgressController.class)
@Import(GlobalExceptionHandler.class)
class ProgressControllerTest {

    private static final Long TEMP_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProgressService progressService;

    @Test
    void completeInitialStudy_shouldReturn200WithProgress() throws Exception {
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        ProgressResponse response = new ProgressResponse();
        response.setProgressId(20L);
        response.setStatus(ProgressStatus.IN_PROGRESS);
        response.setCurrentLevel(1);
        response.setInitialStudiedAt(now);
        response.setLastStudiedAt(now);
        response.setCreatedAt(now);
        response.setUpdatedAt(now);

        when(progressService.completeInitialStudy(TEMP_USER_ID, resourceId)).thenReturn(response);

        mockMvc.perform(post("/resources/{resourceId}/progress/complete-initial-study", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressId").value(20))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.currentLevel").value(1))
                .andExpect(jsonPath("$.initialStudiedAt").value("2026-06-03T10:00:00Z"))
                .andExpect(jsonPath("$.lastStudiedAt").value("2026-06-03T10:00:00Z"));

        verify(progressService).completeInitialStudy(TEMP_USER_ID, resourceId);
    }

    @Test
    void completeInitialStudy_shouldReturn404WhenResourceNotFound() throws Exception {
        Long resourceId = 10L;

        when(progressService.completeInitialStudy(TEMP_USER_ID, resourceId))
                .thenThrow(new ResourceNotFoundException("Resource not found"));

        mockMvc.perform(post("/resources/{resourceId}/progress/complete-initial-study", resourceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));

        verify(progressService).completeInitialStudy(TEMP_USER_ID, resourceId);
    }
}
