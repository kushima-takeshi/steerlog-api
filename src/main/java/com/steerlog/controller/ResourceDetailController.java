package com.steerlog.controller;

import com.steerlog.dto.response.ResourceDetailsResponse;
import com.steerlog.security.CurrentUser;
import com.steerlog.service.ResourceDetailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/resources")
public class ResourceDetailController {

    private final ResourceDetailService resourceDetailService;

    public ResourceDetailController(ResourceDetailService resourceDetailService) {
        this.resourceDetailService = resourceDetailService;
    }

    @GetMapping("/{resourceId}/details")
    public ResponseEntity<ResourceDetailsResponse> getResourceDetails(@PathVariable Long resourceId) {
        ResourceDetailsResponse response = resourceDetailService.getResourceDetails(CurrentUser.requireUserId(), resourceId);
        return ResponseEntity.ok(response);
    }
}
