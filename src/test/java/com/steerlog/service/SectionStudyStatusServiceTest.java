package com.steerlog.service;

import com.steerlog.dto.request.UpdateSectionStudyStatusRequest;
import com.steerlog.dto.response.SectionStudyStatusResponse;
import com.steerlog.entity.LevelHistory;
import com.steerlog.entity.LevelHistoryReasonCode;
import com.steerlog.entity.LevelHistorySourceType;
import com.steerlog.entity.Progress;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.entity.Resource;
import com.steerlog.entity.ResourceSection;
import com.steerlog.entity.ResourceType;
import com.steerlog.entity.SectionStudyStatus;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.repository.LevelHistoryRepository;
import com.steerlog.repository.ProgressRepository;
import com.steerlog.repository.ResourceRepository;
import com.steerlog.repository.ResourceSectionRepository;
import com.steerlog.repository.SectionStudyStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectionStudyStatusServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ResourceSectionRepository resourceSectionRepository;

    @Mock
    private SectionStudyStatusRepository sectionStudyStatusRepository;

    @Mock
    private ProgressRepository progressRepository;

    @Mock
    private LevelHistoryRepository levelHistoryRepository;

    @InjectMocks
    private SectionStudyStatusService sectionStudyStatusService;

    @Test
    void updateStudyStatus_shouldUpdateStudiedAtAndProgress() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;
        Instant studiedAt = Instant.parse("2026-06-05T10:00:00Z");
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        ResourceSection section = buildSection(resourceSectionId, userId, resourceId, before);
        SectionStudyStatus studyStatus = buildStudyStatus(200L, userId, resourceId, resourceSectionId, null, before);
        Progress progress = buildProgress(300L, userId, resourceId, before);

        UpdateSectionStudyStatusRequest request = new UpdateSectionStudyStatusRequest();
        request.setStudiedAt(studiedAt);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.of(section));
        when(sectionStudyStatusRepository.findByUserIdAndResourceSectionId(userId, resourceSectionId))
                .thenReturn(Optional.of(studyStatus));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(sectionStudyStatusRepository.save(any(SectionStudyStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(progressRepository.save(any(Progress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SectionStudyStatusResponse response = sectionStudyStatusService.updateStudyStatus(
                userId, resourceId, resourceSectionId, request);

        ArgumentCaptor<SectionStudyStatus> statusCaptor = ArgumentCaptor.forClass(SectionStudyStatus.class);
        verify(sectionStudyStatusRepository).save(statusCaptor.capture());

        SectionStudyStatus savedStatus = statusCaptor.getValue();
        assertThat(savedStatus.getStudiedAt()).isEqualTo(studiedAt);
        assertThat(savedStatus.getUpdatedAt()).isNotNull();

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        Progress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(savedProgress.getLastStudiedAt()).isNotNull();
        assertThat(savedProgress.getUpdatedAt()).isNotNull();
        assertThat(savedProgress.getCurrentLevel()).isEqualTo(0);

        assertThat(response.getSectionStudyStatusId()).isEqualTo(200L);
        assertThat(response.getResourceId()).isEqualTo(resourceId);
        assertThat(response.getResourceSectionId()).isEqualTo(resourceSectionId);
        assertThat(response.getStudiedAt()).isEqualTo(studiedAt);
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateStudyStatus_shouldNotOverwriteStudiedAtWhenRequestStudiedAtIsNull() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;
        Instant existingStudiedAt = Instant.parse("2026-06-04T10:00:00Z");
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        ResourceSection section = buildSection(resourceSectionId, userId, resourceId, before);
        SectionStudyStatus studyStatus = buildStudyStatus(
                200L, userId, resourceId, resourceSectionId, existingStudiedAt, before);
        Progress progress = buildProgress(300L, userId, resourceId, before);
        progress.setStatus(ProgressStatus.IN_PROGRESS);

        UpdateSectionStudyStatusRequest request = new UpdateSectionStudyStatusRequest();

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.of(section));
        when(sectionStudyStatusRepository.findByUserIdAndResourceSectionId(userId, resourceSectionId))
                .thenReturn(Optional.of(studyStatus));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(sectionStudyStatusRepository.save(any(SectionStudyStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(progressRepository.save(any(Progress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SectionStudyStatusResponse response = sectionStudyStatusService.updateStudyStatus(
                userId, resourceId, resourceSectionId, request);

        ArgumentCaptor<SectionStudyStatus> statusCaptor = ArgumentCaptor.forClass(SectionStudyStatus.class);
        verify(sectionStudyStatusRepository).save(statusCaptor.capture());

        assertThat(statusCaptor.getValue().getStudiedAt()).isEqualTo(existingStudiedAt);
        assertThat(response.getStudiedAt()).isEqualTo(existingStudiedAt);
    }

    @Test
    void updateStudyStatus_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;

        UpdateSectionStudyStatusRequest request = new UpdateSectionStudyStatusRequest();
        request.setStudiedAt(Instant.parse("2026-06-05T10:00:00Z"));

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionStudyStatusService.updateStudyStatus(
                userId, resourceId, resourceSectionId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(resourceSectionRepository, never())
                .findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(any(), any(), any());
        verify(sectionStudyStatusRepository, never()).findByUserIdAndResourceSectionId(any(), any());
        verify(progressRepository, never()).findByUserIdAndResourceId(any(), any());
    }

    @Test
    void updateStudyStatus_shouldThrowResourceNotFoundExceptionWhenSectionNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);

        UpdateSectionStudyStatusRequest request = new UpdateSectionStudyStatusRequest();
        request.setStudiedAt(Instant.parse("2026-06-05T10:00:00Z"));

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionStudyStatusService.updateStudyStatus(
                userId, resourceId, resourceSectionId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(resourceSectionRepository).findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId);
        verify(sectionStudyStatusRepository, never()).findByUserIdAndResourceSectionId(any(), any());
        verify(progressRepository, never()).findByUserIdAndResourceId(any(), any());
    }

    @Test
    void updateStudyStatus_shouldThrowRuntimeExceptionWhenSectionStudyStatusNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        ResourceSection section = buildSection(resourceSectionId, userId, resourceId, before);

        UpdateSectionStudyStatusRequest request = new UpdateSectionStudyStatusRequest();
        request.setStudiedAt(Instant.parse("2026-06-05T10:00:00Z"));

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.of(section));
        when(sectionStudyStatusRepository.findByUserIdAndResourceSectionId(userId, resourceSectionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionStudyStatusService.updateStudyStatus(
                userId, resourceId, resourceSectionId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("SectionStudyStatus not found");

        verify(sectionStudyStatusRepository).findByUserIdAndResourceSectionId(userId, resourceSectionId);
        verify(progressRepository, never()).findByUserIdAndResourceId(any(), any());
        verify(sectionStudyStatusRepository, never()).save(any(SectionStudyStatus.class));
        verify(progressRepository, never()).save(any(Progress.class));
    }

    @Test
    void updateStudyStatus_shouldThrowRuntimeExceptionWhenProgressNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        ResourceSection section = buildSection(resourceSectionId, userId, resourceId, before);
        SectionStudyStatus studyStatus = buildStudyStatus(200L, userId, resourceId, resourceSectionId, null, before);

        UpdateSectionStudyStatusRequest request = new UpdateSectionStudyStatusRequest();
        request.setStudiedAt(Instant.parse("2026-06-05T10:00:00Z"));

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.of(section));
        when(sectionStudyStatusRepository.findByUserIdAndResourceSectionId(userId, resourceSectionId))
                .thenReturn(Optional.of(studyStatus));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionStudyStatusService.updateStudyStatus(
                userId, resourceId, resourceSectionId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Progress not found");

        verify(progressRepository).findByUserIdAndResourceId(userId, resourceId);
        verify(sectionStudyStatusRepository, never()).save(any(SectionStudyStatus.class));
        verify(progressRepository, never()).save(any(Progress.class));
    }

    @Test
    void updateStudyStatus_shouldCompleteLevelOneWhenAllSectionsStudied() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;
        Instant studiedAt = Instant.parse("2026-06-05T10:00:00Z");
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        ResourceSection section1 = buildSection(100L, userId, resourceId, "第1章", 1, before);
        ResourceSection section2 = buildSection(101L, userId, resourceId, "第2章", 2, before);
        SectionStudyStatus studyStatus1 = buildStudyStatus(200L, userId, resourceId, 100L, null, before);
        SectionStudyStatus studyStatus2 = buildStudyStatus(201L, userId, resourceId, 101L, studiedAt, before);
        Progress progress = buildProgress(300L, userId, resourceId, before);

        UpdateSectionStudyStatusRequest request = new UpdateSectionStudyStatusRequest();
        request.setStudiedAt(studiedAt);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.of(section1));
        when(sectionStudyStatusRepository.findByUserIdAndResourceSectionId(userId, resourceSectionId))
                .thenReturn(Optional.of(studyStatus1));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(sectionStudyStatusRepository.save(any(SectionStudyStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(progressRepository.save(any(Progress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resourceSectionRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(
                userId, resourceId))
                .thenReturn(List.of(section1, section2));
        when(sectionStudyStatusRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(List.of(studyStatus1, studyStatus2));
        when(levelHistoryRepository.existsByUserIdAndResourceIdAndLevel(userId, resourceId, 1))
                .thenReturn(false);

        sectionStudyStatusService.updateStudyStatus(userId, resourceId, resourceSectionId, request);

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        Progress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress.getCurrentLevel()).isEqualTo(1);
        assertThat(savedProgress.getInitialStudiedAt()).isNotNull();

        ArgumentCaptor<LevelHistory> levelHistoryCaptor = ArgumentCaptor.forClass(LevelHistory.class);
        verify(levelHistoryRepository).save(levelHistoryCaptor.capture());

        LevelHistory savedLevelHistory = levelHistoryCaptor.getValue();
        assertThat(savedLevelHistory.getUserId()).isEqualTo(userId);
        assertThat(savedLevelHistory.getResourceId()).isEqualTo(resourceId);
        assertThat(savedLevelHistory.getLevel()).isEqualTo(1);
        assertThat(savedLevelHistory.getSourceType()).isEqualTo(LevelHistorySourceType.SECTION_STUDY_STATUS);
        assertThat(savedLevelHistory.getSourceId()).isNull();
        assertThat(savedLevelHistory.getReasonCode()).isEqualTo(LevelHistoryReasonCode.ALL_SECTIONS_STUDIED);
        assertThat(savedLevelHistory.getCreatedAt()).isNotNull();
    }

    @Test
    void updateStudyStatus_shouldNotCompleteLevelOneWhenNotAllSectionsStudied() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;
        Instant studiedAt = Instant.parse("2026-06-05T10:00:00Z");
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        ResourceSection section1 = buildSection(100L, userId, resourceId, "第1章", 1, before);
        ResourceSection section2 = buildSection(101L, userId, resourceId, "第2章", 2, before);
        SectionStudyStatus studyStatus1 = buildStudyStatus(200L, userId, resourceId, 100L, null, before);
        SectionStudyStatus studyStatus2 = buildStudyStatus(201L, userId, resourceId, 101L, null, before);
        Progress progress = buildProgress(300L, userId, resourceId, before);

        UpdateSectionStudyStatusRequest request = new UpdateSectionStudyStatusRequest();
        request.setStudiedAt(studiedAt);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.of(section1));
        when(sectionStudyStatusRepository.findByUserIdAndResourceSectionId(userId, resourceSectionId))
                .thenReturn(Optional.of(studyStatus1));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(sectionStudyStatusRepository.save(any(SectionStudyStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(progressRepository.save(any(Progress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resourceSectionRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(
                userId, resourceId))
                .thenReturn(List.of(section1, section2));
        when(sectionStudyStatusRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(List.of(studyStatus1, studyStatus2));

        sectionStudyStatusService.updateStudyStatus(userId, resourceId, resourceSectionId, request);

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        assertThat(progressCaptor.getValue().getCurrentLevel()).isEqualTo(0);
        assertThat(progressCaptor.getValue().getInitialStudiedAt()).isNull();
        verify(levelHistoryRepository, never()).save(any(LevelHistory.class));
    }

    @Test
    void updateStudyStatus_shouldNotCreateDuplicateLevelHistoryWhenLevelHistoryAlreadyExists() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;
        Instant studiedAt = Instant.parse("2026-06-05T10:00:00Z");
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        ResourceSection section = buildSection(resourceSectionId, userId, resourceId, "第1章", 1, before);
        SectionStudyStatus studyStatus = buildStudyStatus(200L, userId, resourceId, resourceSectionId, null, before);
        Progress progress = buildProgress(300L, userId, resourceId, before);

        UpdateSectionStudyStatusRequest request = new UpdateSectionStudyStatusRequest();
        request.setStudiedAt(studiedAt);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.of(section));
        when(sectionStudyStatusRepository.findByUserIdAndResourceSectionId(userId, resourceSectionId))
                .thenReturn(Optional.of(studyStatus));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(sectionStudyStatusRepository.save(any(SectionStudyStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(progressRepository.save(any(Progress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resourceSectionRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(
                userId, resourceId))
                .thenReturn(List.of(section));
        when(sectionStudyStatusRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(List.of(studyStatus));
        when(levelHistoryRepository.existsByUserIdAndResourceIdAndLevel(userId, resourceId, 1))
                .thenReturn(true);

        sectionStudyStatusService.updateStudyStatus(userId, resourceId, resourceSectionId, request);

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        assertThat(progressCaptor.getValue().getCurrentLevel()).isEqualTo(1);
        verify(levelHistoryRepository, never()).save(any(LevelHistory.class));
    }

    @Test
    void updateStudyStatus_shouldNotDowngradeCurrentLevelWhenAlreadyAboveLevelOne() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;
        Instant studiedAt = Instant.parse("2026-06-05T10:00:00Z");
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        ResourceSection section = buildSection(resourceSectionId, userId, resourceId, "第1章", 1, before);
        SectionStudyStatus studyStatus = buildStudyStatus(200L, userId, resourceId, resourceSectionId, studiedAt, before);
        Progress progress = buildProgress(300L, userId, resourceId, before);
        progress.setCurrentLevel(2);
        progress.setStatus(ProgressStatus.IN_PROGRESS);

        UpdateSectionStudyStatusRequest request = new UpdateSectionStudyStatusRequest();
        request.setStudiedAt(studiedAt);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.of(section));
        when(sectionStudyStatusRepository.findByUserIdAndResourceSectionId(userId, resourceSectionId))
                .thenReturn(Optional.of(studyStatus));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(sectionStudyStatusRepository.save(any(SectionStudyStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(progressRepository.save(any(Progress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resourceSectionRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(
                userId, resourceId))
                .thenReturn(List.of(section));
        when(sectionStudyStatusRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(List.of(studyStatus));
        when(levelHistoryRepository.existsByUserIdAndResourceIdAndLevel(userId, resourceId, 1))
                .thenReturn(false);

        sectionStudyStatusService.updateStudyStatus(userId, resourceId, resourceSectionId, request);

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        assertThat(progressCaptor.getValue().getCurrentLevel()).isEqualTo(2);
        verify(levelHistoryRepository).save(any(LevelHistory.class));
    }

    @Test
    void updateStudyStatus_shouldNotCompleteLevelOneWhenNoSections() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;
        Instant studiedAt = Instant.parse("2026-06-05T10:00:00Z");
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        ResourceSection section = buildSection(resourceSectionId, userId, resourceId, "第1章", 1, before);
        SectionStudyStatus studyStatus = buildStudyStatus(200L, userId, resourceId, resourceSectionId, null, before);
        Progress progress = buildProgress(300L, userId, resourceId, before);

        UpdateSectionStudyStatusRequest request = new UpdateSectionStudyStatusRequest();
        request.setStudiedAt(studiedAt);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.of(section));
        when(sectionStudyStatusRepository.findByUserIdAndResourceSectionId(userId, resourceSectionId))
                .thenReturn(Optional.of(studyStatus));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(sectionStudyStatusRepository.save(any(SectionStudyStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(progressRepository.save(any(Progress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resourceSectionRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(
                userId, resourceId))
                .thenReturn(Collections.emptyList());

        sectionStudyStatusService.updateStudyStatus(userId, resourceId, resourceSectionId, request);

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        assertThat(progressCaptor.getValue().getCurrentLevel()).isEqualTo(0);
        assertThat(progressCaptor.getValue().getInitialStudiedAt()).isNull();
        verify(levelHistoryRepository, never()).existsByUserIdAndResourceIdAndLevel(any(), any(), any());
        verify(levelHistoryRepository, never()).save(any(LevelHistory.class));
    }

    private Resource buildResource(Long resourceId, Long userId, Instant createdAt) {
        Resource resource = new Resource();
        resource.setResourceId(resourceId);
        resource.setUserId(userId);
        resource.setResourceType(ResourceType.BOOK);
        resource.setTitle("Webを支える技術");
        resource.setCreatedAt(createdAt);
        resource.setUpdatedAt(createdAt);
        return resource;
    }

    private ResourceSection buildSection(
            Long resourceSectionId, Long userId, Long resourceId, Instant createdAt) {
        return buildSection(resourceSectionId, userId, resourceId, "第1章", 1, createdAt);
    }

    private ResourceSection buildSection(
            Long resourceSectionId,
            Long userId,
            Long resourceId,
            String title,
            Integer sectionOrder,
            Instant createdAt) {
        ResourceSection section = new ResourceSection();
        section.setResourceSectionId(resourceSectionId);
        section.setUserId(userId);
        section.setResourceId(resourceId);
        section.setTitle(title);
        section.setSectionOrder(sectionOrder);
        section.setCreatedAt(createdAt);
        section.setUpdatedAt(createdAt);
        return section;
    }

    private SectionStudyStatus buildStudyStatus(
            Long sectionStudyStatusId,
            Long userId,
            Long resourceId,
            Long resourceSectionId,
            Instant studiedAt,
            Instant createdAt) {
        SectionStudyStatus studyStatus = new SectionStudyStatus();
        studyStatus.setSectionStudyStatusId(sectionStudyStatusId);
        studyStatus.setUserId(userId);
        studyStatus.setResourceId(resourceId);
        studyStatus.setResourceSectionId(resourceSectionId);
        studyStatus.setStudiedAt(studiedAt);
        studyStatus.setCreatedAt(createdAt);
        studyStatus.setUpdatedAt(createdAt);
        return studyStatus;
    }

    private Progress buildProgress(Long progressId, Long userId, Long resourceId, Instant createdAt) {
        Progress progress = new Progress();
        progress.setProgressId(progressId);
        progress.setUserId(userId);
        progress.setResourceId(resourceId);
        progress.setStatus(ProgressStatus.NOT_STARTED);
        progress.setCurrentLevel(0);
        progress.setCreatedAt(createdAt);
        progress.setUpdatedAt(createdAt);
        return progress;
    }
}
