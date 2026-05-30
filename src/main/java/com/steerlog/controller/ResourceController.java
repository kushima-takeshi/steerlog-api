package com.steerlog.controller;

import com.steerlog.dto.request.CreateResourceRequest;
import com.steerlog.dto.response.CreateResourceResponse;
import com.steerlog.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
