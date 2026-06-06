package com.steerlog.controller;

import com.steerlog.dto.response.LevelHistoryResponse;
import com.steerlog.entity.LevelHistoryReasonCode;
import com.steerlog.entity.LevelHistorySourceType;
import com.steerlog.exception.GlobalExceptionHandler;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.service.LevelHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LevelHistoryController.class)
@Import(GlobalExceptionHandler.class)
class LevelHistoryControllerTest {

    private static final Long TEMP_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LevelHistoryService levelHistoryService;

    @Test
    void getLevelHistories_shouldReturn200WithLevelHistories() throws Exception {
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        LevelHistoryResponse response = new LevelHistoryResponse();
        response.setLevelHistoryId(100L);
        response.setLevel(1);
        response.setSourceType(LevelHistorySourceType.INITIAL_STUDY_COMPLETION);
        response.setSourceId(null);
        response.setReasonCode(LevelHistoryReasonCode.INITIAL_STUDY_COMPLETED);
        response.setCreatedAt(now);

        when(levelHistoryService.getLevelHistories(TEMP_USER_ID, resourceId))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/resources/{resourceId}/level-histories", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].levelHistoryId").value(100))
                .andExpect(jsonPath("$[0].level").value(1))
                .andExpect(jsonPath("$[0].sourceType").value("INITIAL_STUDY_COMPLETION"))
                .andExpect(jsonPath("$[0].reasonCode").value("INITIAL_STUDY_COMPLETED"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-06-03T10:00:00Z"));

        verify(levelHistoryService).getLevelHistories(TEMP_USER_ID, resourceId);
    }

    @Test
    void getLevelHistories_shouldReturn200WithEmptyList() throws Exception {
        Long resourceId = 10L;

        when(levelHistoryService.getLevelHistories(TEMP_USER_ID, resourceId))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/resources/{resourceId}/level-histories", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(levelHistoryService).getLevelHistories(TEMP_USER_ID, resourceId);
    }

    @Test
    void getLevelHistories_shouldReturn404WhenResourceNotFound() throws Exception {
        Long resourceId = 10L;

        when(levelHistoryService.getLevelHistories(TEMP_USER_ID, resourceId))
                .thenThrow(new ResourceNotFoundException("Resource not found"));

        mockMvc.perform(get("/resources/{resourceId}/level-histories", resourceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));

        verify(levelHistoryService).getLevelHistories(TEMP_USER_ID, resourceId);
    }
}
