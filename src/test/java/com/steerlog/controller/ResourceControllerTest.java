package com.steerlog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steerlog.dto.request.CreateResourceRequest;
import com.steerlog.dto.request.UpdateResourceRequest;
import com.steerlog.dto.response.CreateResourceResponse;
import com.steerlog.dto.response.ProgressResponse;
import com.steerlog.dto.response.ResourceWithProgressResponse;
import com.steerlog.dto.response.ResourceListItemResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    void getResources_shouldReturn200WithResourceList() throws Exception {
        ResourceListItemResponse item = buildResourceListItemResponse(10L, "Webを支える技術");

        when(resourceService.getResources(TEMP_USER_ID)).thenReturn(List.of(item));

        mockMvc.perform(get("/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resourceId").value(10))
                .andExpect(jsonPath("$[0].title").value("Webを支える技術"))
                .andExpect(jsonPath("$[0].progress.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$[0].progress.currentLevel").value(0));

        verify(resourceService).getResources(TEMP_USER_ID);
    }

    @Test
    void getResources_shouldReturn200WithEmptyList() throws Exception {
        when(resourceService.getResources(TEMP_USER_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(resourceService).getResources(TEMP_USER_ID);
    }

    @Test
    void getResourceDetail_shouldReturn200WithResourceAndProgress() throws Exception {
        Long resourceId = 10L;
        ResourceWithProgressResponse response = buildResourceWithProgressResponse(resourceId, "Webを支える技術");

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
    void updateResource_shouldReturn200WithUpdatedResource() throws Exception {
        Long resourceId = 10L;
        UpdateResourceRequest request = new UpdateResourceRequest();
        request.setTitle("更新後タイトル");

        ResourceWithProgressResponse response = buildResourceWithProgressResponse(resourceId, "更新後タイトル");

        when(resourceService.updateResource(eq(TEMP_USER_ID), eq(resourceId), any(UpdateResourceRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/resources/{resourceId}", resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceId").value(10))
                .andExpect(jsonPath("$.title").value("更新後タイトル"))
                .andExpect(jsonPath("$.progress.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.progress.currentLevel").value(0));

        verify(resourceService).updateResource(eq(TEMP_USER_ID), eq(resourceId), any(UpdateResourceRequest.class));
    }

    @Test
    void updateResource_shouldReturn404WhenResourceNotFound() throws Exception {
        Long resourceId = 10L;
        UpdateResourceRequest request = new UpdateResourceRequest();
        request.setTitle("更新後タイトル");

        when(resourceService.updateResource(eq(TEMP_USER_ID), eq(resourceId), any(UpdateResourceRequest.class)))
                .thenThrow(new ResourceNotFoundException("Resource not found"));

        mockMvc.perform(patch("/resources/{resourceId}", resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));

        verify(resourceService).updateResource(eq(TEMP_USER_ID), eq(resourceId), any(UpdateResourceRequest.class));
    }

    @Test
    void deleteResource_shouldReturn204() throws Exception {
        Long resourceId = 10L;

        doNothing().when(resourceService).deleteResource(TEMP_USER_ID, resourceId);

        mockMvc.perform(delete("/resources/{resourceId}", resourceId))
                .andExpect(status().isNoContent());

        verify(resourceService).deleteResource(TEMP_USER_ID, resourceId);
    }

    @Test
    void deleteResource_shouldReturn404WhenResourceNotFound() throws Exception {
        Long resourceId = 10L;

        doThrow(new ResourceNotFoundException("Resource not found"))
                .when(resourceService).deleteResource(TEMP_USER_ID, resourceId);

        mockMvc.perform(delete("/resources/{resourceId}", resourceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));

        verify(resourceService).deleteResource(TEMP_USER_ID, resourceId);
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

    private ResourceListItemResponse buildResourceListItemResponse(Long resourceId, String title) {
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        ResourceListItemResponse response = new ResourceListItemResponse();
        response.setResourceId(resourceId);
        response.setResourceType(ResourceType.BOOK);
        response.setTitle(title);
        response.setCreatedAt(now);
        response.setUpdatedAt(now);
        response.setProgress(buildProgressResponse(20L, now));
        return response;
    }

    private ResourceWithProgressResponse buildResourceWithProgressResponse(Long resourceId, String title) {
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        ResourceWithProgressResponse response = new ResourceWithProgressResponse();
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
