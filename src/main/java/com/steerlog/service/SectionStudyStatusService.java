package com.steerlog.service;

import com.steerlog.dto.request.UpdateSectionStudyStatusRequest;
import com.steerlog.dto.response.SectionStudyStatusResponse;
import com.steerlog.entity.Progress;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.entity.SectionStudyStatus;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.repository.ProgressRepository;
import com.steerlog.repository.ResourceRepository;
import com.steerlog.repository.ResourceSectionRepository;
import com.steerlog.repository.SectionStudyStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class SectionStudyStatusService {

    private final ResourceRepository resourceRepository;
    private final ResourceSectionRepository resourceSectionRepository;
    private final SectionStudyStatusRepository sectionStudyStatusRepository;
    private final ProgressRepository progressRepository;

    public SectionStudyStatusService(
            ResourceRepository resourceRepository,
            ResourceSectionRepository resourceSectionRepository,
            SectionStudyStatusRepository sectionStudyStatusRepository,
            ProgressRepository progressRepository) {
        this.resourceRepository = resourceRepository;
        this.resourceSectionRepository = resourceSectionRepository;
        this.sectionStudyStatusRepository = sectionStudyStatusRepository;
        this.progressRepository = progressRepository;
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
        progressRepository.save(progress);

        return toSectionStudyStatusResponse(savedStudyStatus);
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
