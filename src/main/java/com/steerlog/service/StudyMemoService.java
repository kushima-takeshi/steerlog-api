package com.steerlog.service;

import com.steerlog.dto.request.CreateStudyMemoRequest;
import com.steerlog.dto.request.UpdateStudyMemoRequest;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        memo.setTags(formatTags(request.getTags()));
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

    @Transactional(readOnly = true)
    public StudyMemoResponse getMemo(Long userId, Long resourceId, Long studyMemoId) {
        resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        StudyMemo memo = studyMemoRepository
                .findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(studyMemoId, userId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        return toStudyMemoResponse(memo);
    }

    @Transactional
    public void deleteMemo(Long userId, Long resourceId, Long studyMemoId) {
        resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        StudyMemo memo = studyMemoRepository
                .findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(studyMemoId, userId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        Instant now = Instant.now();
        memo.setDeletedAt(now);
        memo.setUpdatedAt(now);
        studyMemoRepository.save(memo);
    }

    @Transactional
    public StudyMemoResponse updateMemo(
            Long userId, Long resourceId, Long studyMemoId, UpdateStudyMemoRequest request) {
        resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        StudyMemo memo = studyMemoRepository
                .findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(studyMemoId, userId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        Instant now = Instant.now();

        if (request.getMemoType() != null) {
            memo.setMemoType(request.getMemoType());
        }
        if (request.getContent() != null) {
            memo.setContent(request.getContent());
        }
        if (request.getTags() != null) {
            memo.setTags(formatTags(request.getTags()));
        }
        memo.setUpdatedAt(now);

        StudyMemo savedMemo = studyMemoRepository.save(memo);

        return toStudyMemoResponse(savedMemo);
    }

    private String formatTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return String.join(",", tags);
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }

    private StudyMemoResponse toStudyMemoResponse(StudyMemo memo) {
        StudyMemoResponse response = new StudyMemoResponse();
        response.setStudyMemoId(memo.getStudyMemoId());
        response.setResourceId(memo.getResourceId());
        response.setResourceSectionId(memo.getResourceSectionId());
        response.setMemoType(memo.getMemoType());
        response.setContent(memo.getContent());
        response.setTags(parseTags(memo.getTags()));
        response.setCreatedAt(memo.getCreatedAt());
        response.setUpdatedAt(memo.getUpdatedAt());
        return response;
    }
}
