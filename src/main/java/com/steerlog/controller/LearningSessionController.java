package com.steerlog.controller;

import com.steerlog.dto.request.SaveLearningSessionRecordRequest;
import com.steerlog.dto.request.StartLearningSessionRequest;
import com.steerlog.dto.request.SubmitLearningSessionResponseRequest;
import com.steerlog.dto.response.CompleteLearningSessionResponse;
import com.steerlog.dto.response.DiscardLearningSessionResponse;
import com.steerlog.dto.response.LearningSessionRecordResponse;
import com.steerlog.dto.response.LearningSessionResponse;
import com.steerlog.dto.response.SubmitLearningSessionResponseResponse;
import com.steerlog.service.LearningSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/resources/{resourceId}/learning-sessions")
public class LearningSessionController {

    private static final Long TEMP_USER_ID = 1L;

    private final LearningSessionService learningSessionService;

    public LearningSessionController(LearningSessionService learningSessionService) {
        this.learningSessionService = learningSessionService;
    }

    @PostMapping
    public ResponseEntity<LearningSessionResponse> startSession(
            @PathVariable Long resourceId,
            @Valid @RequestBody StartLearningSessionRequest request) {
        LearningSessionResponse response =
                learningSessionService.startSession(TEMP_USER_ID, resourceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{learningSessionId}/discard")
    public ResponseEntity<DiscardLearningSessionResponse> discardSession(
            @PathVariable Long resourceId,
            @PathVariable Long learningSessionId) {
        DiscardLearningSessionResponse response =
                learningSessionService.discardSession(TEMP_USER_ID, resourceId, learningSessionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{learningSessionId}/responses")
    public ResponseEntity<SubmitLearningSessionResponseResponse> submitResponse(
            @PathVariable Long resourceId,
            @PathVariable Long learningSessionId,
            @Valid @RequestBody SubmitLearningSessionResponseRequest request) {
        SubmitLearningSessionResponseResponse response = learningSessionService.submitResponse(
                TEMP_USER_ID, resourceId, learningSessionId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{learningSessionId}/complete")
    public ResponseEntity<CompleteLearningSessionResponse> completeSession(
            @PathVariable Long resourceId,
            @PathVariable Long learningSessionId) {
        CompleteLearningSessionResponse response =
                learningSessionService.completeSession(TEMP_USER_ID, resourceId, learningSessionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{learningSessionId}/record")
    public ResponseEntity<LearningSessionRecordResponse> saveRecord(
            @PathVariable Long resourceId,
            @PathVariable Long learningSessionId,
            @Valid @RequestBody SaveLearningSessionRecordRequest request) {
        LearningSessionRecordResponse response = learningSessionService.saveRecord(
                TEMP_USER_ID, resourceId, learningSessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
