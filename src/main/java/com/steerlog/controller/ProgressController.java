package com.steerlog.controller;

import com.steerlog.dto.response.ProgressResponse;
import com.steerlog.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/resources/{resourceId}/progress")
public class ProgressController {

    private static final Long TEMP_USER_ID = 1L;

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @PostMapping("/complete-initial-study")
    public ResponseEntity<ProgressResponse> completeInitialStudy(@PathVariable Long resourceId) {
        ProgressResponse response = progressService.completeInitialStudy(TEMP_USER_ID, resourceId);
        return ResponseEntity.ok(response);
    }
}
