package com.steerlog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steerlog.dto.request.CreateResourceRequest;
import com.steerlog.dto.response.CreateResourceResponse;
import com.steerlog.dto.response.ProgressResponse;
import com.steerlog.dto.response.ResourceDetailResponse;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.entity.ResourceType;
import com.steerlog.exception.GlobalExceptionHandler;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourceController.class)
@Import(GlobalExceptionHandler.class)
class ResourceControllerTest {

    private static final Long TEMP_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ResourceService resourceService;

    @Test
    void createResource_shouldReturn201WithResourceAndProgress() throws Exception {
        CreateResourceRequest request = new CreateResourceRequest();
        request.setResourceType(ResourceType.BOOK);
        request.setTitle("Webを支える技術");
        request.setAuthor("山本陽平");

        CreateResourceResponse response = buildCreateResourceResponse(10L, "Webを支える技術");

        when(resourceService.createResource(eq(TEMP_USER_ID), any(CreateResourceRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceId").value(10))
                .andExpect(jsonPath("$.title").value("Webを支える技術"))
                .andExpect(jsonPath("$.progress.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.progress.currentLevel").value(0));

        verify(resourceService).createResource(eq(TEMP_USER_ID), any(CreateResourceRequest.class));
    }

    @Test
    void getResourceDetail_shouldReturn200WithResourceAndProgress() throws Exception {
        Long resourceId = 10L;
        ResourceDetailResponse response = buildResourceDetailResponse(resourceId, "Webを支える技術");

        when(resourceService.getResourceDetail(TEMP_USER_ID, resourceId)).thenReturn(response);

        mockMvc.perform(get("/resources/{resourceId}", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceId").value(10))
                .andExpect(jsonPath("$.title").value("Webを支える技術"))
                .andExpect(jsonPath("$.progress.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.progress.currentLevel").value(0));

        verify(resourceService).getResourceDetail(TEMP_USER_ID, resourceId);
    }

    @Test
    void getResourceDetail_shouldReturn404WhenResourceNotFound() throws Exception {
        Long resourceId = 10L;

        when(resourceService.getResourceDetail(TEMP_USER_ID, resourceId))
                .thenThrow(new ResourceNotFoundException("Resource not found"));

        mockMvc.perform(get("/resources/{resourceId}", resourceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));

        verify(resourceService).getResourceDetail(TEMP_USER_ID, resourceId);
    }

    private CreateResourceResponse buildCreateResourceResponse(Long resourceId, String title) {
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        CreateResourceResponse response = new CreateResourceResponse();
        response.setResourceId(resourceId);
        response.setResourceType(ResourceType.BOOK);
        response.setTitle(title);
        response.setCreatedAt(now);
        response.setUpdatedAt(now);
        response.setProgress(buildProgressResponse(20L, now));
        return response;
    }

    private ResourceDetailResponse buildResourceDetailResponse(Long resourceId, String title) {
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        ResourceDetailResponse response = new ResourceDetailResponse();
        response.setResourceId(resourceId);
        response.setResourceType(ResourceType.BOOK);
        response.setTitle(title);
        response.setCreatedAt(now);
        response.setUpdatedAt(now);
        response.setProgress(buildProgressResponse(20L, now));
        return response;
    }

    private ProgressResponse buildProgressResponse(Long progressId, Instant now) {
        ProgressResponse progress = new ProgressResponse();
        progress.setProgressId(progressId);
        progress.setStatus(ProgressStatus.NOT_STARTED);
        progress.setCurrentLevel(0);
        progress.setCreatedAt(now);
        progress.setUpdatedAt(now);
        return progress;
    }
}
