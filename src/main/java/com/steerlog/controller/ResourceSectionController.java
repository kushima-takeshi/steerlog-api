package com.steerlog.controller;

import com.steerlog.dto.request.CreateResourceSectionRequest;
import com.steerlog.dto.request.UpdateResourceSectionRequest;
import com.steerlog.dto.response.ResourceSectionResponse;
import com.steerlog.service.ResourceSectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/resources/{resourceId}/sections")
public class ResourceSectionController {

    private static final Long TEMP_USER_ID = 1L;

    private final ResourceSectionService resourceSectionService;

    public ResourceSectionController(ResourceSectionService resourceSectionService) {
        this.resourceSectionService = resourceSectionService;
    }

    @PostMapping
    public ResponseEntity<ResourceSectionResponse> createSection(
            @PathVariable Long resourceId,
            @Valid @RequestBody CreateResourceSectionRequest request) {
        ResourceSectionResponse response =
                resourceSectionService.createSection(TEMP_USER_ID, resourceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{sectionId}")
    public ResponseEntity<ResourceSectionResponse> updateSection(
            @PathVariable Long resourceId,
            @PathVariable Long sectionId,
            @Valid @RequestBody UpdateResourceSectionRequest request) {
        ResourceSectionResponse response =
                resourceSectionService.updateSection(TEMP_USER_ID, resourceId, sectionId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<Void> deleteSection(
            @PathVariable Long resourceId,
            @PathVariable Long sectionId) {
        resourceSectionService.deleteSection(TEMP_USER_ID, resourceId, sectionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ResourceSectionResponse>> getSections(@PathVariable Long resourceId) {
        List<ResourceSectionResponse> responses = resourceSectionService.getSections(TEMP_USER_ID, resourceId);
        return ResponseEntity.ok(responses);
    }
}
