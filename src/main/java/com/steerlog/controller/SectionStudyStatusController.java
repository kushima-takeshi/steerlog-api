package com.steerlog.controller;

import com.steerlog.dto.request.UpdateSectionStudyStatusRequest;
import com.steerlog.dto.response.SectionStudyStatusResponse;
import com.steerlog.security.CurrentUser;
import com.steerlog.service.SectionStudyStatusService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/resources/{resourceId}/sections/{sectionId}/study-status")
public class SectionStudyStatusController {

    private final SectionStudyStatusService sectionStudyStatusService;

    public SectionStudyStatusController(SectionStudyStatusService sectionStudyStatusService) {
        this.sectionStudyStatusService = sectionStudyStatusService;
    }

    @GetMapping
    public ResponseEntity<SectionStudyStatusResponse> getStudyStatus(
            @PathVariable Long resourceId,
            @PathVariable Long sectionId) {
        SectionStudyStatusResponse response =
                sectionStudyStatusService.getStudyStatus(CurrentUser.requireUserId(), resourceId, sectionId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping
    public ResponseEntity<SectionStudyStatusResponse> updateStudyStatus(
            @PathVariable Long resourceId,
            @PathVariable Long sectionId,
            @Valid @RequestBody UpdateSectionStudyStatusRequest request) {
        SectionStudyStatusResponse response = sectionStudyStatusService.updateStudyStatus(
                CurrentUser.requireUserId(), resourceId, sectionId, request);
        return ResponseEntity.ok(response);
    }
}
