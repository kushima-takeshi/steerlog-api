package com.steerlog.controller;

import com.steerlog.dto.request.CreateStudyMemoRequest;
import com.steerlog.dto.response.StudyMemoResponse;
import com.steerlog.service.StudyMemoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/resources/{resourceId}/memos")
public class StudyMemoController {

    private static final Long TEMP_USER_ID = 1L;

    private final StudyMemoService studyMemoService;

    public StudyMemoController(StudyMemoService studyMemoService) {
        this.studyMemoService = studyMemoService;
    }

    @PostMapping
    public ResponseEntity<StudyMemoResponse> createMemo(
            @PathVariable Long resourceId,
            @Valid @RequestBody CreateStudyMemoRequest request) {
        StudyMemoResponse response = studyMemoService.createMemo(TEMP_USER_ID, resourceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<StudyMemoResponse>> getMemos(@PathVariable Long resourceId) {
        List<StudyMemoResponse> responses = studyMemoService.getMemos(TEMP_USER_ID, resourceId);
        return ResponseEntity.ok(responses);
    }
}
