package com.steerlog.service;

import com.steerlog.dto.request.UpdateSectionStudyStatusRequest;
import com.steerlog.dto.response.SectionStudyStatusResponse;
import com.steerlog.entity.LevelHistory;
import com.steerlog.entity.LevelHistoryReasonCode;
import com.steerlog.entity.LevelHistorySourceType;
import com.steerlog.entity.Progress;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.entity.ResourceSection;
import com.steerlog.entity.SectionStudyStatus;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.repository.LevelHistoryRepository;
import com.steerlog.repository.ProgressRepository;
import com.steerlog.repository.ResourceRepository;
import com.steerlog.repository.ResourceSectionRepository;
import com.steerlog.repository.SectionStudyStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SectionStudyStatusService {

    private final ResourceRepository resourceRepository;
    private final ResourceSectionRepository resourceSectionRepository;
    private final SectionStudyStatusRepository sectionStudyStatusRepository;
    private final ProgressRepository progressRepository;
    private final LevelHistoryRepository levelHistoryRepository;

    public SectionStudyStatusService(
            ResourceRepository resourceRepository,
            ResourceSectionRepository resourceSectionRepository,
            SectionStudyStatusRepository sectionStudyStatusRepository,
            ProgressRepository progressRepository,
            LevelHistoryRepository levelHistoryRepository) {
        this.resourceRepository = resourceRepository;
        this.resourceSectionRepository = resourceSectionRepository;
        this.sectionStudyStatusRepository = sectionStudyStatusRepository;
        this.progressRepository = progressRepository;
        this.levelHistoryRepository = levelHistoryRepository;
    }

    @Transactional
    public SectionStudyStatusResponse updateStudyStatus(
            Long userId,
            Long resourceId,
            Long resourceSectionId,
            UpdateSectionStudyStatusRequest request) {
        resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        resourceSectionRepository
                .findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                        resourceSectionId, userId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        SectionStudyStatus studyStatus = sectionStudyStatusRepository
                .findByUserIdAndResourceSectionId(userId, resourceSectionId)
                .orElseThrow(() -> new RuntimeException("SectionStudyStatus not found"));

        Progress progress = progressRepository
                .findByUserIdAndResourceId(userId, resourceId)
                .orElseThrow(() -> new RuntimeException("Progress not found"));

        Instant now = Instant.now();

        if (request.getStudiedAt() != null) {
            studyStatus.setStudiedAt(request.getStudiedAt());
        }
        studyStatus.setUpdatedAt(now);

        if (progress.getStatus() == ProgressStatus.NOT_STARTED) {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
        }
        progress.setLastStudiedAt(now);
        progress.setUpdatedAt(now);

        SectionStudyStatus savedStudyStatus = sectionStudyStatusRepository.save(studyStatus);
        completeLevelOneIfNeeded(userId, resourceId, progress, now);
        progressRepository.save(progress);

        return toSectionStudyStatusResponse(savedStudyStatus);
    }

    private void completeLevelOneIfNeeded(Long userId, Long resourceId, Progress progress, Instant now) {
        if (!isAllSectionsStudied(userId, resourceId)) {
            return;
        }

        if (progress.getCurrentLevel() == 0) {
            progress.setCurrentLevel(1);
        }
        if (progress.getInitialStudiedAt() == null) {
            progress.setInitialStudiedAt(now);
        }

        if (!levelHistoryRepository.existsByUserIdAndResourceIdAndLevel(userId, resourceId, 1)) {
            LevelHistory levelHistory = new LevelHistory();
            levelHistory.setUserId(userId);
            levelHistory.setResourceId(resourceId);
            levelHistory.setLevel(1);
            levelHistory.setSourceType(LevelHistorySourceType.SECTION_STUDY_STATUS);
            levelHistory.setSourceId(null);
            levelHistory.setReasonCode(LevelHistoryReasonCode.ALL_SECTIONS_STUDIED);
            levelHistory.setCreatedAt(now);
            levelHistoryRepository.save(levelHistory);
        }
    }

    private boolean isAllSectionsStudied(Long userId, Long resourceId) {
        List<ResourceSection> sections = resourceSectionRepository
                .findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(userId, resourceId);
        if (sections.isEmpty()) {
            return false;
        }

        List<SectionStudyStatus> studyStatuses =
                sectionStudyStatusRepository.findByUserIdAndResourceId(userId, resourceId);
        Map<Long, SectionStudyStatus> studyStatusBySectionId = studyStatuses.stream()
                .collect(Collectors.toMap(SectionStudyStatus::getResourceSectionId, status -> status));

        for (ResourceSection section : sections) {
            SectionStudyStatus studyStatus = studyStatusBySectionId.get(section.getResourceSectionId());
            if (studyStatus == null || studyStatus.getStudiedAt() == null) {
                return false;
            }
        }
        return true;
    }

    private SectionStudyStatusResponse toSectionStudyStatusResponse(SectionStudyStatus studyStatus) {
        SectionStudyStatusResponse response = new SectionStudyStatusResponse();
        response.setSectionStudyStatusId(studyStatus.getSectionStudyStatusId());
        response.setResourceId(studyStatus.getResourceId());
        response.setResourceSectionId(studyStatus.getResourceSectionId());
        response.setStudiedAt(studyStatus.getStudiedAt());
        response.setCreatedAt(studyStatus.getCreatedAt());
        response.setUpdatedAt(studyStatus.getUpdatedAt());
        return response;
    }
}
