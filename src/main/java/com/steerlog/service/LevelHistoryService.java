package com.steerlog.service;

import com.steerlog.dto.response.LevelHistoryResponse;
import com.steerlog.entity.LevelHistory;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.repository.LevelHistoryRepository;
import com.steerlog.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LevelHistoryService {

    private final ResourceRepository resourceRepository;
    private final LevelHistoryRepository levelHistoryRepository;

    public LevelHistoryService(
            ResourceRepository resourceRepository,
            LevelHistoryRepository levelHistoryRepository) {
        this.resourceRepository = resourceRepository;
        this.levelHistoryRepository = levelHistoryRepository;
    }

    @Transactional(readOnly = true)
    public List<LevelHistoryResponse> getLevelHistories(Long userId, Long resourceId) {
        resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        List<LevelHistory> levelHistories =
                levelHistoryRepository.findByUserIdAndResourceIdOrderByCreatedAtAsc(userId, resourceId);

        return levelHistories.stream()
                .map(this::toLevelHistoryResponse)
                .toList();
    }

    private LevelHistoryResponse toLevelHistoryResponse(LevelHistory levelHistory) {
        LevelHistoryResponse response = new LevelHistoryResponse();
        response.setLevelHistoryId(levelHistory.getLevelHistoryId());
        response.setLevel(levelHistory.getLevel());
        response.setSourceType(levelHistory.getSourceType());
        response.setSourceId(levelHistory.getSourceId());
        response.setReasonCode(levelHistory.getReasonCode());
        response.setCreatedAt(levelHistory.getCreatedAt());
        return response;
    }
}
