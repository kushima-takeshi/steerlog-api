package com.steerlog.service;

import com.steerlog.dto.request.CreateStudyMemoRequest;
import com.steerlog.dto.response.StudyMemoResponse;
import com.steerlog.entity.Progress;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.entity.StudyMemo;
import com.steerlog.entity.StudyMemoType;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.repository.ProgressRepository;
import com.steerlog.repository.ResourceRepository;
import com.steerlog.repository.ResourceSectionRepository;
import com.steerlog.repository.StudyMemoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class StudyMemoService {

    private final ResourceRepository resourceRepository;
    private final ResourceSectionRepository resourceSectionRepository;
    private final StudyMemoRepository studyMemoRepository;
    private final ProgressRepository progressRepository;

    public StudyMemoService(
            ResourceRepository resourceRepository,
            ResourceSectionRepository resourceSectionRepository,
            StudyMemoRepository studyMemoRepository,
            ProgressRepository progressRepository) {
        this.resourceRepository = resourceRepository;
        this.resourceSectionRepository = resourceSectionRepository;
        this.studyMemoRepository = studyMemoRepository;
        this.progressRepository = progressRepository;
    }

    @Transactional
    public StudyMemoResponse createMemo(Long userId, Long resourceId, CreateStudyMemoRequest request) {
        resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        if (request.getResourceSectionId() != null) {
            resourceSectionRepository
                    .findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                            request.getResourceSectionId(), userId, resourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        }

        Progress progress = progressRepository
                .findByUserIdAndResourceId(userId, resourceId)
                .orElseThrow(() -> new RuntimeException("Progress not found"));

        Instant now = Instant.now();

        StudyMemo memo = new StudyMemo();
        memo.setUserId(userId);
        memo.setResourceId(resourceId);
        memo.setResourceSectionId(request.getResourceSectionId());
        memo.setMemoType(request.getMemoType() != null ? request.getMemoType() : StudyMemoType.GENERAL);
        memo.setContent(request.getContent());
        memo.setDeletedAt(null);
        memo.setCreatedAt(now);
        memo.setUpdatedAt(now);

        StudyMemo savedMemo = studyMemoRepository.save(memo);

        if (progress.getStatus() == ProgressStatus.NOT_STARTED) {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
        }
        progress.setLastStudiedAt(now);
        progress.setUpdatedAt(now);
        progressRepository.save(progress);

        return toStudyMemoResponse(savedMemo);
    }

    @Transactional(readOnly = true)
    public List<StudyMemoResponse> getMemos(Long userId, Long resourceId) {
        resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        List<StudyMemo> memos = studyMemoRepository
                .findByUserIdAndResourceIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, resourceId);

        return memos.stream()
                .map(this::toStudyMemoResponse)
                .toList();
    }

    private StudyMemoResponse toStudyMemoResponse(StudyMemo memo) {
        StudyMemoResponse response = new StudyMemoResponse();
        response.setStudyMemoId(memo.getStudyMemoId());
        response.setResourceId(memo.getResourceId());
        response.setResourceSectionId(memo.getResourceSectionId());
        response.setMemoType(memo.getMemoType());
        response.setContent(memo.getContent());
        response.setCreatedAt(memo.getCreatedAt());
        response.setUpdatedAt(memo.getUpdatedAt());
        return response;
    }
}
