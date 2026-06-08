package com.steerlog.controller;

import com.steerlog.dto.request.StartLearningSessionRequest;
import com.steerlog.dto.response.LearningSessionResponse;
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
}
