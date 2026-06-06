package com.steerlog.service;

import com.steerlog.dto.response.ProgressResponse;
import com.steerlog.entity.LevelHistory;
import com.steerlog.entity.LevelHistoryReasonCode;
import com.steerlog.entity.LevelHistorySourceType;
import com.steerlog.entity.Progress;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.repository.LevelHistoryRepository;
import com.steerlog.repository.ProgressRepository;
import com.steerlog.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ProgressService {

    private final ResourceRepository resourceRepository;
    private final ProgressRepository progressRepository;
    private final LevelHistoryRepository levelHistoryRepository;

    public ProgressService(
            ResourceRepository resourceRepository,
            ProgressRepository progressRepository,
            LevelHistoryRepository levelHistoryRepository) {
        this.resourceRepository = resourceRepository;
        this.progressRepository = progressRepository;
        this.levelHistoryRepository = levelHistoryRepository;
    }

    @Transactional(readOnly = true)
    public ProgressResponse getProgress(Long userId, Long resourceId) {
        resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        Progress progress = progressRepository
                .findByUserIdAndResourceId(userId, resourceId)
                .orElseThrow(() -> new RuntimeException("Progress not found"));

        return toProgressResponse(progress);
    }

    @Transactional
    public ProgressResponse completeInitialStudy(Long userId, Long resourceId) {
        resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        Progress progress = progressRepository
                .findByUserIdAndResourceId(userId, resourceId)
                .orElseThrow(() -> new RuntimeException("Progress not found"));

        Instant now = Instant.now();
        progress.setInitialStudiedAt(now);
        progress.setLastStudiedAt(now);
        if (progress.getStatus() == ProgressStatus.NOT_STARTED) {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
        }
        if (progress.getCurrentLevel() == 0) {
            progress.setCurrentLevel(1);
        }
        progress.setUpdatedAt(now);

        Progress savedProgress = progressRepository.save(progress);

        if (!levelHistoryRepository.existsByUserIdAndResourceIdAndLevel(userId, resourceId, 1)) {
            LevelHistory levelHistory = new LevelHistory();
            levelHistory.setUserId(userId);
            levelHistory.setResourceId(resourceId);
            levelHistory.setLevel(1);
            levelHistory.setSourceType(LevelHistorySourceType.INITIAL_STUDY_COMPLETION);
            levelHistory.setSourceId(null);
            levelHistory.setReasonCode(LevelHistoryReasonCode.INITIAL_STUDY_COMPLETED);
            levelHistory.setCreatedAt(now);
            levelHistoryRepository.save(levelHistory);
        }

        return toProgressResponse(savedProgress);
    }

    private ProgressResponse toProgressResponse(Progress progress) {
        ProgressResponse response = new ProgressResponse();
        response.setProgressId(progress.getProgressId());
        response.setStatus(progress.getStatus());
        response.setCurrentLevel(progress.getCurrentLevel());
        response.setCurrentSectionId(progress.getCurrentSectionId());
        response.setStartedAt(progress.getStartedAt());
        response.setCompletedAt(progress.getCompletedAt());
        response.setArchivedAt(progress.getArchivedAt());
        response.setArchiveReason(progress.getArchiveReason());
        response.setInitialStudiedAt(progress.getInitialStudiedAt());
        response.setLastStudiedAt(progress.getLastStudiedAt());
        response.setCreatedAt(progress.getCreatedAt());
        response.setUpdatedAt(progress.getUpdatedAt());
        return response;
    }
}
