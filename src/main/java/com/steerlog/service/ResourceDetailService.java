package com.steerlog.service;

import com.steerlog.dto.response.LevelHistorySummaryResponse;
import com.steerlog.dto.response.LearningSessionRecordSummaryResponse;
import com.steerlog.dto.response.ProgressSummaryResponse;
import com.steerlog.dto.response.ResourceDetailsResponse;
import com.steerlog.dto.response.ResourceSectionWithStudyStatusResponse;
import com.steerlog.dto.response.ResourceSummaryResponse;
import com.steerlog.dto.response.SectionStudyStatusSummaryResponse;
import com.steerlog.dto.response.StudyMemoSummaryResponse;
import com.steerlog.entity.LevelHistory;
import com.steerlog.entity.LearningSessionRecord;
import com.steerlog.entity.Progress;
import com.steerlog.entity.Resource;
import com.steerlog.entity.ResourceSection;
import com.steerlog.entity.SectionStudyStatus;
import com.steerlog.entity.StudyMemo;
import com.steerlog.exception.ProgressNotFoundException;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.repository.LevelHistoryRepository;
import com.steerlog.repository.LearningSessionRecordRepository;
import com.steerlog.repository.ProgressRepository;
import com.steerlog.repository.ResourceRepository;
import com.steerlog.repository.ResourceSectionRepository;
import com.steerlog.repository.SectionStudyStatusRepository;
import com.steerlog.repository.StudyMemoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ResourceDetailService {

    private final ResourceRepository resourceRepository;
    private final ProgressRepository progressRepository;
    private final ResourceSectionRepository resourceSectionRepository;
    private final SectionStudyStatusRepository sectionStudyStatusRepository;
    private final StudyMemoRepository studyMemoRepository;
    private final LevelHistoryRepository levelHistoryRepository;
    private final LearningSessionRecordRepository learningSessionRecordRepository;

    public ResourceDetailService(
            ResourceRepository resourceRepository,
            ProgressRepository progressRepository,
            ResourceSectionRepository resourceSectionRepository,
            SectionStudyStatusRepository sectionStudyStatusRepository,
            StudyMemoRepository studyMemoRepository,
            LevelHistoryRepository levelHistoryRepository,
            LearningSessionRecordRepository learningSessionRecordRepository) {
        this.resourceRepository = resourceRepository;
        this.progressRepository = progressRepository;
        this.resourceSectionRepository = resourceSectionRepository;
        this.sectionStudyStatusRepository = sectionStudyStatusRepository;
        this.studyMemoRepository = studyMemoRepository;
        this.levelHistoryRepository = levelHistoryRepository;
        this.learningSessionRecordRepository = learningSessionRecordRepository;
    }

    @Transactional(readOnly = true)
    public ResourceDetailsResponse getResourceDetails(Long userId, Long resourceId) {
        Resource resource = resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        Progress progress = progressRepository
                .findByUserIdAndResourceId(userId, resourceId)
                .orElseThrow(() -> new ProgressNotFoundException("Progress not found"));

        List<ResourceSection> sections = resourceSectionRepository
                .findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(userId, resourceId);

        List<SectionStudyStatus> studyStatuses =
                sectionStudyStatusRepository.findByUserIdAndResourceId(userId, resourceId);
        Map<Long, SectionStudyStatus> studyStatusBySectionId = studyStatuses.stream()
                .collect(Collectors.toMap(SectionStudyStatus::getResourceSectionId, status -> status));

        List<StudyMemo> memos = studyMemoRepository
                .findByUserIdAndResourceIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, resourceId);

        List<LevelHistory> levelHistories =
                levelHistoryRepository.findByUserIdAndResourceIdOrderByCreatedAtAsc(userId, resourceId);

        List<LearningSessionRecord> records = learningSessionRecordRepository
                .findByUserIdAndResourceIdOrderByCreatedAtDesc(userId, resourceId);

        ResourceDetailsResponse response = new ResourceDetailsResponse();
        response.setResource(toResourceSummaryResponse(resource));
        response.setProgress(toProgressSummaryResponse(progress));
        response.setSections(sections.stream()
                .map(section -> toResourceSectionWithStudyStatusResponse(section, studyStatusBySectionId))
                .toList());
        response.setMemos(memos.stream()
                .map(this::toStudyMemoSummaryResponse)
                .toList());
        response.setLevelHistories(levelHistories.stream()
                .map(this::toLevelHistorySummaryResponse)
                .toList());
        response.setLearningSessionRecords(records.stream()
                .map(this::toLearningSessionRecordSummaryResponse)
                .toList());
        return response;
    }

    private ResourceSummaryResponse toResourceSummaryResponse(Resource resource) {
        ResourceSummaryResponse response = new ResourceSummaryResponse();
        response.setResourceId(resource.getResourceId());
        response.setResourceType(resource.getResourceType());
        response.setTitle(resource.getTitle());
        response.setAuthor(resource.getAuthor());
        response.setSourceUrl(resource.getSourceUrl());
        response.setDescription(resource.getDescription());
        return response;
    }

    private ProgressSummaryResponse toProgressSummaryResponse(Progress progress) {
        ProgressSummaryResponse response = new ProgressSummaryResponse();
        response.setStatus(progress.getStatus());
        response.setCurrentLevel(progress.getCurrentLevel());
        response.setCurrentSectionId(progress.getCurrentSectionId());
        response.setInitialStudiedAt(progress.getInitialStudiedAt());
        response.setLastStudiedAt(progress.getLastStudiedAt());
        return response;
    }

    private ResourceSectionWithStudyStatusResponse toResourceSectionWithStudyStatusResponse(
            ResourceSection section, Map<Long, SectionStudyStatus> studyStatusBySectionId) {
        ResourceSectionWithStudyStatusResponse response = new ResourceSectionWithStudyStatusResponse();
        response.setResourceSectionId(section.getResourceSectionId());
        response.setTitle(section.getTitle());
        response.setSectionOrder(section.getSectionOrder());

        SectionStudyStatusSummaryResponse studyStatusResponse = new SectionStudyStatusSummaryResponse();
        SectionStudyStatus studyStatus = studyStatusBySectionId.get(section.getResourceSectionId());
        if (studyStatus != null) {
            studyStatusResponse.setStudiedAt(studyStatus.getStudiedAt());
        }
        response.setStudyStatus(studyStatusResponse);
        return response;
    }

    private StudyMemoSummaryResponse toStudyMemoSummaryResponse(StudyMemo memo) {
        StudyMemoSummaryResponse response = new StudyMemoSummaryResponse();
        response.setStudyMemoId(memo.getStudyMemoId());
        response.setResourceSectionId(memo.getResourceSectionId());
        response.setMemoType(memo.getMemoType());
        response.setContent(memo.getContent());
        response.setCreatedAt(memo.getCreatedAt());
        return response;
    }

    private LevelHistorySummaryResponse toLevelHistorySummaryResponse(LevelHistory levelHistory) {
        LevelHistorySummaryResponse response = new LevelHistorySummaryResponse();
        response.setLevel(levelHistory.getLevel());
        response.setSourceType(levelHistory.getSourceType());
        response.setSourceId(levelHistory.getSourceId());
        response.setReasonCode(levelHistory.getReasonCode());
        response.setCreatedAt(levelHistory.getCreatedAt());
        return response;
    }

    private LearningSessionRecordSummaryResponse toLearningSessionRecordSummaryResponse(
            LearningSessionRecord record) {
        LearningSessionRecordSummaryResponse response = new LearningSessionRecordSummaryResponse();
        response.setLearningSessionRecordId(record.getLearningSessionRecordId());
        response.setSessionType(record.getSessionType());
        response.setSummary(record.getSummary());
        response.setConceptTags(parseConceptTags(record.getConceptTags()));
        response.setWeakPointSummary(record.getWeakPointSummary());
        response.setNextAction(record.getNextAction());
        response.setAiAssessment(record.getAiAssessment());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }

    private List<String> parseConceptTags(String conceptTags) {
        if (conceptTags == null || conceptTags.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(conceptTags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }
}
