package com.steerlog.controller;

import com.steerlog.dto.request.CreateResourceRequest;
import com.steerlog.dto.request.UpdateResourceRequest;
import com.steerlog.dto.response.CreateResourceResponse;
import com.steerlog.dto.response.ResourceWithProgressResponse;
import com.steerlog.dto.response.ResourceListItemResponse;
import com.steerlog.service.ResourceService;
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
@RequestMapping("/resources")
public class ResourceController {

    private static final Long TEMP_USER_ID = 1L;

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    public ResponseEntity<CreateResourceResponse> createResource(
            @Valid @RequestBody CreateResourceRequest request) {
        CreateResourceResponse response = resourceService.createResource(TEMP_USER_ID, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ResourceListItemResponse>> getResources() {
        List<ResourceListItemResponse> responses = resourceService.getResources(TEMP_USER_ID);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<ResourceWithProgressResponse> getResourceDetail(@PathVariable Long resourceId) {
        ResourceWithProgressResponse response = resourceService.getResourceDetail(TEMP_USER_ID, resourceId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{resourceId}")
    public ResponseEntity<ResourceWithProgressResponse> updateResource(
            @PathVariable Long resourceId,
            @Valid @RequestBody UpdateResourceRequest request) {
        ResourceWithProgressResponse response = resourceService.updateResource(TEMP_USER_ID, resourceId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<Void> deleteResource(@PathVariable Long resourceId) {
        resourceService.deleteResource(TEMP_USER_ID, resourceId);
        return ResponseEntity.noContent().build();
    }
}
