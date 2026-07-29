package com.steerlog.service;

import com.steerlog.dto.request.UpdateProgressRequest;
import com.steerlog.dto.response.ProgressResponse;
import com.steerlog.entity.LevelHistory;
import com.steerlog.entity.LevelHistoryReasonCode;
import com.steerlog.entity.LevelHistorySourceType;
import com.steerlog.entity.Progress;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.exception.InvalidProgressStatusTransitionException;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.repository.LevelHistoryRepository;
import com.steerlog.repository.ProgressRepository;
import com.steerlog.repository.ResourceRepository;
import com.steerlog.repository.ResourceSectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ProgressService {

    private final ResourceRepository resourceRepository;
    private final ProgressRepository progressRepository;
    private final LevelHistoryRepository levelHistoryRepository;
    private final ResourceSectionRepository resourceSectionRepository;

    public ProgressService(
            ResourceRepository resourceRepository,
            ProgressRepository progressRepository,
            LevelHistoryRepository levelHistoryRepository,
            ResourceSectionRepository resourceSectionRepository) {
        this.resourceRepository = resourceRepository;
        this.progressRepository = progressRepository;
        this.levelHistoryRepository = levelHistoryRepository;
        this.resourceSectionRepository = resourceSectionRepository;
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

    @Transactional
    public ProgressResponse updateProgress(Long userId, Long resourceId, UpdateProgressRequest request) {
        resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        Progress progress = progressRepository
                .findByUserIdAndResourceId(userId, resourceId)
                .orElseThrow(() -> new RuntimeException("Progress not found"));

        if (request.getCurrentSectionId() != null) {
            resourceSectionRepository
                    .findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                            request.getCurrentSectionId(), userId, resourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        }

        Instant now = Instant.now();

        if (request.getStatus() != null) {
            applyStatusTransition(progress, request.getStatus(), request.getArchiveReason(), now);
        }

        if (request.getCurrentSectionId() != null) {
            progress.setCurrentSectionId(request.getCurrentSectionId());
        }

        if (request.getArchiveReason() != null) {
            progress.setArchiveReason(request.getArchiveReason());
        }

        progress.setUpdatedAt(now);
        Progress savedProgress = progressRepository.save(progress);

        return toProgressResponse(savedProgress);
    }

    private void applyStatusTransition(
            Progress progress, ProgressStatus targetStatus, String archiveReason, Instant now) {
        ProgressStatus currentStatus = progress.getStatus();

        if (currentStatus == targetStatus) {
            return;
        }

        if (targetStatus == ProgressStatus.ARCHIVED) {
            progress.setStatus(ProgressStatus.ARCHIVED);
            progress.setArchivedAt(now);
            if (archiveReason != null) {
                progress.setArchiveReason(archiveReason);
            }
            return;
        }

        if (currentStatus == ProgressStatus.NOT_STARTED && targetStatus == ProgressStatus.IN_PROGRESS) {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
            if (progress.getStartedAt() == null) {
                progress.setStartedAt(now);
            }
            progress.setLastStudiedAt(now);
            return;
        }

        if (currentStatus == ProgressStatus.IN_PROGRESS && targetStatus == ProgressStatus.PAUSED) {
            progress.setStatus(ProgressStatus.PAUSED);
            return;
        }

        if (currentStatus == ProgressStatus.PAUSED && targetStatus == ProgressStatus.IN_PROGRESS) {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
            progress.setLastStudiedAt(now);
            return;
        }

        if (currentStatus == ProgressStatus.ARCHIVED
                && (targetStatus == ProgressStatus.IN_PROGRESS
                        || targetStatus == ProgressStatus.PAUSED
                        || targetStatus == ProgressStatus.NOT_STARTED)) {
            progress.setStatus(targetStatus);
            return;
        }

        throw new InvalidProgressStatusTransitionException("Invalid progress status transition");
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
