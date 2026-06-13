package com.steerlog.service;

import com.steerlog.dto.response.ResourceDetailsResponse;
import com.steerlog.entity.LevelHistory;
import com.steerlog.entity.LevelHistoryReasonCode;
import com.steerlog.entity.LevelHistorySourceType;
import com.steerlog.entity.LearningSessionAiAssessment;
import com.steerlog.entity.LearningSessionRecord;
import com.steerlog.entity.LearningSessionType;
import com.steerlog.entity.Progress;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.entity.Resource;
import com.steerlog.entity.ResourceSection;
import com.steerlog.entity.ResourceType;
import com.steerlog.entity.SectionStudyStatus;
import com.steerlog.entity.StudyMemo;
import com.steerlog.entity.StudyMemoType;
import com.steerlog.exception.ProgressNotFoundException;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.repository.LevelHistoryRepository;
import com.steerlog.repository.LearningSessionRecordRepository;
import com.steerlog.repository.ProgressRepository;
import com.steerlog.repository.ResourceRepository;
import com.steerlog.repository.ResourceSectionRepository;
import com.steerlog.repository.SectionStudyStatusRepository;
import com.steerlog.repository.StudyMemoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceDetailServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ProgressRepository progressRepository;

    @Mock
    private ResourceSectionRepository resourceSectionRepository;

    @Mock
    private SectionStudyStatusRepository sectionStudyStatusRepository;

    @Mock
    private StudyMemoRepository studyMemoRepository;

    @Mock
    private LevelHistoryRepository levelHistoryRepository;

    @Mock
    private LearningSessionRecordRepository learningSessionRecordRepository;

    @InjectMocks
    private ResourceDetailService resourceDetailService;

    @Test
    void getResourceDetails_shouldReturnAggregatedDetails() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-11T10:00:00Z");
        Instant earlier = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, earlier);
        Progress progress = buildProgress(userId, resourceId, now);
        ResourceSection section1 = buildSection(100L, userId, resourceId, "第2章", 2, earlier);
        ResourceSection section2 = buildSection(101L, userId, resourceId, "第1章", 1, earlier);
        SectionStudyStatus studyStatus1 = buildStudyStatus(200L, userId, resourceId, 100L, now);
        SectionStudyStatus studyStatus2 = buildStudyStatus(201L, userId, resourceId, 101L, earlier);
        StudyMemo memo = buildMemo(300L, userId, resourceId, 100L, now);
        LevelHistory levelHistory = buildLevelHistory(userId, resourceId, 1, earlier);
        LearningSessionRecord record = buildRecord(400L, userId, resourceId, now);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(resourceSectionRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(
                userId, resourceId))
                .thenReturn(List.of(section2, section1));
        when(sectionStudyStatusRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(List.of(studyStatus1, studyStatus2));
        when(studyMemoRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                userId, resourceId))
                .thenReturn(List.of(memo));
        when(levelHistoryRepository.findByUserIdAndResourceIdOrderByCreatedAtAsc(userId, resourceId))
                .thenReturn(List.of(levelHistory));
        when(learningSessionRecordRepository.findByUserIdAndResourceIdOrderByCreatedAtDesc(userId, resourceId))
                .thenReturn(List.of(record));

        ResourceDetailsResponse response = resourceDetailService.getResourceDetails(userId, resourceId);

        assertThat(response.getResource().getResourceId()).isEqualTo(resourceId);
        assertThat(response.getResource().getTitle()).isEqualTo("Webを支える技術");
        assertThat(response.getProgress().getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(response.getProgress().getCurrentLevel()).isEqualTo(3);

        assertThat(response.getSections()).hasSize(2);
        assertThat(response.getSections().get(0).getResourceSectionId()).isEqualTo(101L);
        assertThat(response.getSections().get(0).getSectionOrder()).isEqualTo(1);
        assertThat(response.getSections().get(0).getStudyStatus().getStudiedAt()).isEqualTo(earlier);
        assertThat(response.getSections().get(1).getResourceSectionId()).isEqualTo(100L);
        assertThat(response.getSections().get(1).getStudyStatus().getStudiedAt()).isEqualTo(now);

        assertThat(response.getMemos()).hasSize(1);
        assertThat(response.getMemos().get(0).getStudyMemoId()).isEqualTo(300L);

        assertThat(response.getLevelHistories()).hasSize(1);
        assertThat(response.getLevelHistories().get(0).getLevel()).isEqualTo(1);
        assertThat(response.getLevelHistories().get(0).getSourceId()).isNull();

        assertThat(response.getLearningSessionRecords()).hasSize(1);
        assertThat(response.getLearningSessionRecords().get(0).getLearningSessionRecordId()).isEqualTo(400L);
        assertThat(response.getLearningSessionRecords().get(0).getConceptTags())
                .containsExactly("REST", "HTTP");
    }

    @Test
    void getResourceDetails_shouldReturnEmptyArraysWhenNoChildData() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, now);
        Progress progress = buildProgress(userId, resourceId, now);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(resourceSectionRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(
                userId, resourceId))
                .thenReturn(Collections.emptyList());
        when(sectionStudyStatusRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Collections.emptyList());
        when(studyMemoRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                userId, resourceId))
                .thenReturn(Collections.emptyList());
        when(levelHistoryRepository.findByUserIdAndResourceIdOrderByCreatedAtAsc(userId, resourceId))
                .thenReturn(Collections.emptyList());
        when(learningSessionRecordRepository.findByUserIdAndResourceIdOrderByCreatedAtDesc(userId, resourceId))
                .thenReturn(Collections.emptyList());

        ResourceDetailsResponse response = resourceDetailService.getResourceDetails(userId, resourceId);

        assertThat(response.getSections()).isEmpty();
        assertThat(response.getMemos()).isEmpty();
        assertThat(response.getLevelHistories()).isEmpty();
        assertThat(response.getLearningSessionRecords()).isEmpty();
    }

    @Test
    void getResourceDetails_shouldPreserveRepositoryOrdering() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant older = Instant.parse("2026-06-01T10:00:00Z");
        Instant newer = Instant.parse("2026-06-11T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, older);
        Progress progress = buildProgress(userId, resourceId, newer);
        StudyMemo memoOlder = buildMemo(300L, userId, resourceId, null, older);
        StudyMemo memoNewer = buildMemo(301L, userId, resourceId, null, newer);
        LevelHistory levelHistoryOlder = buildLevelHistory(userId, resourceId, 1, older);
        LevelHistory levelHistoryNewer = buildLevelHistory(userId, resourceId, 2, newer);
        LearningSessionRecord recordOlder = buildRecord(400L, userId, resourceId, older);
        LearningSessionRecord recordNewer = buildRecord(401L, userId, resourceId, newer);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(resourceSectionRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(
                userId, resourceId))
                .thenReturn(Collections.emptyList());
        when(sectionStudyStatusRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Collections.emptyList());
        when(studyMemoRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                userId, resourceId))
                .thenReturn(List.of(memoNewer, memoOlder));
        when(levelHistoryRepository.findByUserIdAndResourceIdOrderByCreatedAtAsc(userId, resourceId))
                .thenReturn(List.of(levelHistoryOlder, levelHistoryNewer));
        when(learningSessionRecordRepository.findByUserIdAndResourceIdOrderByCreatedAtDesc(userId, resourceId))
                .thenReturn(List.of(recordNewer, recordOlder));

        ResourceDetailsResponse response = resourceDetailService.getResourceDetails(userId, resourceId);

        assertThat(response.getMemos().get(0).getStudyMemoId()).isEqualTo(301L);
        assertThat(response.getMemos().get(1).getStudyMemoId()).isEqualTo(300L);
        assertThat(response.getLevelHistories().get(0).getLevel()).isEqualTo(1);
        assertThat(response.getLevelHistories().get(1).getLevel()).isEqualTo(2);
        assertThat(response.getLearningSessionRecords().get(0).getLearningSessionRecordId()).isEqualTo(401L);
        assertThat(response.getLearningSessionRecords().get(1).getLearningSessionRecordId()).isEqualTo(400L);
    }

    @Test
    void getResourceDetails_shouldReturnNullStudiedAtWhenStudyStatusMissing() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, now);
        Progress progress = buildProgress(userId, resourceId, now);
        ResourceSection section = buildSection(100L, userId, resourceId, "第1章", 1, now);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(resourceSectionRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(
                userId, resourceId))
                .thenReturn(List.of(section));
        when(sectionStudyStatusRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Collections.emptyList());
        when(studyMemoRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                userId, resourceId))
                .thenReturn(Collections.emptyList());
        when(levelHistoryRepository.findByUserIdAndResourceIdOrderByCreatedAtAsc(userId, resourceId))
                .thenReturn(Collections.emptyList());
        when(learningSessionRecordRepository.findByUserIdAndResourceIdOrderByCreatedAtDesc(userId, resourceId))
                .thenReturn(Collections.emptyList());

        ResourceDetailsResponse response = resourceDetailService.getResourceDetails(userId, resourceId);

        assertThat(response.getSections()).hasSize(1);
        assertThat(response.getSections().get(0).getStudyStatus()).isNotNull();
        assertThat(response.getSections().get(0).getStudyStatus().getStudiedAt()).isNull();
    }

    @Test
    void getResourceDetails_shouldParseConceptTagsFromCommaSeparatedString() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-11T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, now);
        Progress progress = buildProgress(userId, resourceId, now);
        LearningSessionRecord record = buildRecord(400L, userId, resourceId, now);
        record.setConceptTags(" REST , HTTP , ");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(resourceSectionRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(
                userId, resourceId))
                .thenReturn(Collections.emptyList());
        when(sectionStudyStatusRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Collections.emptyList());
        when(studyMemoRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                userId, resourceId))
                .thenReturn(Collections.emptyList());
        when(levelHistoryRepository.findByUserIdAndResourceIdOrderByCreatedAtAsc(userId, resourceId))
                .thenReturn(Collections.emptyList());
        when(learningSessionRecordRepository.findByUserIdAndResourceIdOrderByCreatedAtDesc(userId, resourceId))
                .thenReturn(List.of(record));

        ResourceDetailsResponse response = resourceDetailService.getResourceDetails(userId, resourceId);

        assertThat(response.getLearningSessionRecords().get(0).getConceptTags())
                .containsExactly("REST", "HTTP");
    }

    @Test
    void getResourceDetails_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceDetailService.getResourceDetails(userId, resourceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(progressRepository, never()).findByUserIdAndResourceId(userId, resourceId);
    }

    @Test
    void getResourceDetails_shouldThrowProgressNotFoundExceptionWhenProgressNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, now);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceDetailService.getResourceDetails(userId, resourceId))
                .isInstanceOf(ProgressNotFoundException.class)
                .hasMessage("Progress not found");

        verify(resourceSectionRepository, never())
                .findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(userId, resourceId);
    }

    private Resource buildResource(Long resourceId, Long userId, Instant createdAt) {
        Resource resource = new Resource();
        resource.setResourceId(resourceId);
        resource.setUserId(userId);
        resource.setResourceType(ResourceType.BOOK);
        resource.setTitle("Webを支える技術");
        resource.setAuthor("山本陽平");
        resource.setCreatedAt(createdAt);
        resource.setUpdatedAt(createdAt);
        return resource;
    }

    private Progress buildProgress(Long userId, Long resourceId, Instant now) {
        Progress progress = new Progress();
        progress.setProgressId(50L);
        progress.setUserId(userId);
        progress.setResourceId(resourceId);
        progress.setStatus(ProgressStatus.IN_PROGRESS);
        progress.setCurrentLevel(3);
        progress.setCurrentSectionId(100L);
        progress.setInitialStudiedAt(now);
        progress.setLastStudiedAt(now);
        progress.setCreatedAt(now);
        progress.setUpdatedAt(now);
        return progress;
    }

    private ResourceSection buildSection(
            Long sectionId, Long userId, Long resourceId, String title, int sectionOrder, Instant now) {
        ResourceSection section = new ResourceSection();
        section.setResourceSectionId(sectionId);
        section.setUserId(userId);
        section.setResourceId(resourceId);
        section.setTitle(title);
        section.setSectionOrder(sectionOrder);
        section.setCreatedAt(now);
        section.setUpdatedAt(now);
        return section;
    }

    private SectionStudyStatus buildStudyStatus(
            Long statusId, Long userId, Long resourceId, Long sectionId, Instant studiedAt) {
        SectionStudyStatus studyStatus = new SectionStudyStatus();
        studyStatus.setSectionStudyStatusId(statusId);
        studyStatus.setUserId(userId);
        studyStatus.setResourceId(resourceId);
        studyStatus.setResourceSectionId(sectionId);
        studyStatus.setStudiedAt(studiedAt);
        studyStatus.setCreatedAt(studiedAt);
        studyStatus.setUpdatedAt(studiedAt);
        return studyStatus;
    }

    private StudyMemo buildMemo(
            Long memoId, Long userId, Long resourceId, Long sectionId, Instant createdAt) {
        StudyMemo memo = new StudyMemo();
        memo.setStudyMemoId(memoId);
        memo.setUserId(userId);
        memo.setResourceId(resourceId);
        memo.setResourceSectionId(sectionId);
        memo.setMemoType(StudyMemoType.QUESTION);
        memo.setContent("PUTとPATCHの違いがまだ曖昧");
        memo.setCreatedAt(createdAt);
        memo.setUpdatedAt(createdAt);
        return memo;
    }

    private LevelHistory buildLevelHistory(Long userId, Long resourceId, int level, Instant createdAt) {
        LevelHistory levelHistory = new LevelHistory();
        levelHistory.setLevelHistoryId((long) level);
        levelHistory.setUserId(userId);
        levelHistory.setResourceId(resourceId);
        levelHistory.setLevel(level);
        levelHistory.setSourceType(LevelHistorySourceType.INITIAL_STUDY_COMPLETION);
        levelHistory.setSourceId(null);
        levelHistory.setReasonCode(LevelHistoryReasonCode.INITIAL_STUDY_COMPLETED);
        levelHistory.setCreatedAt(createdAt);
        return levelHistory;
    }

    private LearningSessionRecord buildRecord(Long recordId, Long userId, Long resourceId, Instant createdAt) {
        LearningSessionRecord record = new LearningSessionRecord();
        record.setLearningSessionRecordId(recordId);
        record.setUserId(userId);
        record.setResourceId(resourceId);
        record.setLearningSessionId(500L);
        record.setSessionType(LearningSessionType.IMMEDIATE_REFLECTION);
        record.setSummary("REST APIの基本を説明できた");
        record.setConceptTags("REST,HTTP");
        record.setWeakPointSummary("PATCHの使い分けが曖昧");
        record.setNextAction("Progress更新APIでPATCH設計を整理する");
        record.setAiAssessment(LearningSessionAiAssessment.NEEDS_REVIEW);
        record.setCreatedAt(createdAt);
        record.setUpdatedAt(createdAt);
        return record;
    }
}
