package com.steerlog.controller;

import com.steerlog.dto.response.LevelHistoryResponse;
import com.steerlog.service.LevelHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/resources/{resourceId}/level-histories")
public class LevelHistoryController {

    private static final Long TEMP_USER_ID = 1L;

    private final LevelHistoryService levelHistoryService;

    public LevelHistoryController(LevelHistoryService levelHistoryService) {
        this.levelHistoryService = levelHistoryService;
    }

    @GetMapping
    public ResponseEntity<List<LevelHistoryResponse>> getLevelHistories(@PathVariable Long resourceId) {
        List<LevelHistoryResponse> responses = levelHistoryService.getLevelHistories(TEMP_USER_ID, resourceId);
        return ResponseEntity.ok(responses);
    }
}
