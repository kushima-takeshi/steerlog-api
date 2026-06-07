package com.steerlog.service;

import com.steerlog.dto.request.UpdateSectionStudyStatusRequest;
import com.steerlog.dto.response.SectionStudyStatusResponse;
import com.steerlog.entity.Progress;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.entity.Resource;
import com.steerlog.entity.ResourceSection;
import com.steerlog.entity.ResourceType;
import com.steerlog.entity.SectionStudyStatus;
import com.steerlog.exception.ResourceNotFoundException;
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
        ResourceSection section = new ResourceSection();
        section.setResourceSectionId(resourceSectionId);
        section.setUserId(userId);
        section.setResourceId(resourceId);
        section.setTitle("第1章");
        section.setSectionOrder(1);
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
