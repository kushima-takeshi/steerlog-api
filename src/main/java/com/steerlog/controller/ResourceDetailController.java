package com.steerlog.controller;

import com.steerlog.dto.response.ResourceDetailsResponse;
import com.steerlog.service.ResourceDetailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/resources")
public class ResourceDetailController {

    private static final Long TEMP_USER_ID = 1L;

    private final ResourceDetailService resourceDetailService;

    public ResourceDetailController(ResourceDetailService resourceDetailService) {
        this.resourceDetailService = resourceDetailService;
    }

    @GetMapping("/{resourceId}/details")
    public ResponseEntity<ResourceDetailsResponse> getResourceDetails(@PathVariable Long resourceId) {
        ResourceDetailsResponse response = resourceDetailService.getResourceDetails(TEMP_USER_ID, resourceId);
        return ResponseEntity.ok(response);
    }
}
