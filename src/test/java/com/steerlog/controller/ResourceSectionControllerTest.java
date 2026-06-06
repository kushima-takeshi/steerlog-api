package com.steerlog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steerlog.dto.request.CreateResourceSectionRequest;
import com.steerlog.dto.response.ResourceSectionResponse;
import com.steerlog.exception.GlobalExceptionHandler;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.service.ResourceSectionService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourceSectionController.class)
@Import(GlobalExceptionHandler.class)
class ResourceSectionControllerTest {

    private static final Long TEMP_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ResourceSectionService resourceSectionService;

    @Test
    void createSection_shouldReturn201() throws Exception {
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        CreateResourceSectionRequest request = new CreateResourceSectionRequest();
        request.setTitle("第1章");
        request.setSectionOrder(1);

        ResourceSectionResponse response = new ResourceSectionResponse();
        response.setResourceSectionId(100L);
        response.setResourceId(resourceId);
        response.setTitle("第1章");
        response.setSectionOrder(1);
        response.setCreatedAt(now);
        response.setUpdatedAt(now);

        when(resourceSectionService.createSection(eq(TEMP_USER_ID), eq(resourceId), any(CreateResourceSectionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/resources/{resourceId}/sections", resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceSectionId").value(100))
                .andExpect(jsonPath("$.title").value("第1章"))
                .andExpect(jsonPath("$.sectionOrder").value(1));

        verify(resourceSectionService).createSection(eq(TEMP_USER_ID), eq(resourceId), any(CreateResourceSectionRequest.class));
    }

    @Test
    void getSections_shouldReturn200WithSections() throws Exception {
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        ResourceSectionResponse response = new ResourceSectionResponse();
        response.setResourceSectionId(100L);
        response.setResourceId(resourceId);
        response.setTitle("第1章");
        response.setSectionOrder(1);
        response.setCreatedAt(now);
        response.setUpdatedAt(now);

        when(resourceSectionService.getSections(TEMP_USER_ID, resourceId))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/resources/{resourceId}/sections", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resourceSectionId").value(100))
                .andExpect(jsonPath("$[0].title").value("第1章"))
                .andExpect(jsonPath("$[0].sectionOrder").value(1));

        verify(resourceSectionService).getSections(TEMP_USER_ID, resourceId);
    }

    @Test
    void getSections_shouldReturn200WithEmptyList() throws Exception {
        Long resourceId = 10L;

        when(resourceSectionService.getSections(TEMP_USER_ID, resourceId))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/resources/{resourceId}/sections", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(resourceSectionService).getSections(TEMP_USER_ID, resourceId);
    }

    @Test
    void createSection_shouldReturn404WhenResourceNotFound() throws Exception {
        Long resourceId = 10L;

        CreateResourceSectionRequest request = new CreateResourceSectionRequest();
        request.setTitle("第1章");
        request.setSectionOrder(1);

        when(resourceSectionService.createSection(eq(TEMP_USER_ID), eq(resourceId), any(CreateResourceSectionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Resource not found"));

        mockMvc.perform(post("/resources/{resourceId}/sections", resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));

        verify(resourceSectionService).createSection(eq(TEMP_USER_ID), eq(resourceId), any(CreateResourceSectionRequest.class));
    }
}
