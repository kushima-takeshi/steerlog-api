package com.steerlog.service;

import com.steerlog.dto.request.UpdateProgressRequest;
import com.steerlog.dto.response.ProgressResponse;
import com.steerlog.entity.LevelHistory;
import com.steerlog.entity.LevelHistoryReasonCode;
import com.steerlog.entity.LevelHistorySourceType;
import com.steerlog.entity.Progress;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.entity.Resource;
import com.steerlog.entity.ResourceSection;
import com.steerlog.entity.ResourceType;
import com.steerlog.exception.InvalidProgressStatusTransitionException;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.repository.LevelHistoryRepository;
import com.steerlog.repository.ProgressRepository;
import com.steerlog.repository.ResourceRepository;
import com.steerlog.repository.ResourceSectionRepository;
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
class ProgressServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ProgressRepository progressRepository;

    @Mock
    private LevelHistoryRepository levelHistoryRepository;

    @Mock
    private ResourceSectionRepository resourceSectionRepository;

    @InjectMocks
    private ProgressService progressService;

    @Test
    void getProgress_shouldReturnProgress() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, now);
        Progress progress = buildProgress(20L, userId, resourceId, now);
        progress.setStatus(ProgressStatus.IN_PROGRESS);
        progress.setCurrentLevel(1);
        progress.setInitialStudiedAt(now);
        progress.setLastStudiedAt(now);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));

        ProgressResponse response = progressService.getProgress(userId, resourceId);

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(progressRepository).findByUserIdAndResourceId(userId, resourceId);
        verify(progressRepository, never()).save(any(Progress.class));
        verify(levelHistoryRepository, never()).existsByUserIdAndResourceIdAndLevel(any(), any(), any());
        verify(levelHistoryRepository, never()).save(any(LevelHistory.class));

        assertThat(response.getProgressId()).isEqualTo(20L);
        assertThat(response.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(response.getCurrentLevel()).isEqualTo(1);
        assertThat(response.getInitialStudiedAt()).isEqualTo(now);
        assertThat(response.getLastStudiedAt()).isEqualTo(now);
    }

    @Test
    void getProgress_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressService.getProgress(userId, resourceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(progressRepository, never()).findByUserIdAndResourceId(any(), any());
    }

    @Test
    void getProgress_shouldThrowRuntimeExceptionWhenProgressNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, now);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressService.getProgress(userId, resourceId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Progress not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(progressRepository).findByUserIdAndResourceId(userId, resourceId);
        verify(progressRepository, never()).save(any(Progress.class));
    }

    @Test
    void completeInitialStudy_shouldUpdateProgressAndCreateLevelHistory() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        Progress progress = buildProgress(20L, userId, resourceId, before);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(progressRepository.save(any(Progress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(levelHistoryRepository.existsByUserIdAndResourceIdAndLevel(userId, resourceId, 1))
                .thenReturn(false);

        ProgressResponse response = progressService.completeInitialStudy(userId, resourceId);

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        Progress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress.getInitialStudiedAt()).isNotNull();
        assertThat(savedProgress.getLastStudiedAt()).isNotNull();
        assertThat(savedProgress.getUpdatedAt()).isNotNull();
        assertThat(savedProgress.getCurrentLevel()).isEqualTo(1);
        assertThat(savedProgress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);

        ArgumentCaptor<LevelHistory> levelHistoryCaptor = ArgumentCaptor.forClass(LevelHistory.class);
        verify(levelHistoryRepository).save(levelHistoryCaptor.capture());

        LevelHistory savedLevelHistory = levelHistoryCaptor.getValue();
        assertThat(savedLevelHistory.getUserId()).isEqualTo(userId);
        assertThat(savedLevelHistory.getResourceId()).isEqualTo(resourceId);
        assertThat(savedLevelHistory.getLevel()).isEqualTo(1);
        assertThat(savedLevelHistory.getSourceType()).isEqualTo(LevelHistorySourceType.INITIAL_STUDY_COMPLETION);
        assertThat(savedLevelHistory.getSourceId()).isNull();
        assertThat(savedLevelHistory.getReasonCode()).isEqualTo(LevelHistoryReasonCode.INITIAL_STUDY_COMPLETED);
        assertThat(savedLevelHistory.getCreatedAt()).isNotNull();

        assertThat(response.getProgressId()).isEqualTo(20L);
        assertThat(response.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(response.getCurrentLevel()).isEqualTo(1);
        assertThat(response.getInitialStudiedAt()).isNotNull();
        assertThat(response.getLastStudiedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void completeInitialStudy_shouldNotCreateDuplicateLevelHistoryWhenAlreadyExists() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        Progress progress = buildProgress(20L, userId, resourceId, before);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(progressRepository.save(any(Progress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(levelHistoryRepository.existsByUserIdAndResourceIdAndLevel(userId, resourceId, 1))
                .thenReturn(true);

        ProgressResponse response = progressService.completeInitialStudy(userId, resourceId);

        verify(progressRepository).save(any(Progress.class));
        verify(levelHistoryRepository, never()).save(any(LevelHistory.class));

        assertThat(response.getCurrentLevel()).isEqualTo(1);
        assertThat(response.getInitialStudiedAt()).isNotNull();
        assertThat(response.getLastStudiedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void completeInitialStudy_shouldNotDowngradeCurrentLevel() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        Progress progress = buildProgress(20L, userId, resourceId, before);
        progress.setCurrentLevel(2);
        progress.setStatus(ProgressStatus.IN_PROGRESS);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(progressRepository.save(any(Progress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(levelHistoryRepository.existsByUserIdAndResourceIdAndLevel(userId, resourceId, 1))
                .thenReturn(true);

        ProgressResponse response = progressService.completeInitialStudy(userId, resourceId);

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        assertThat(progressCaptor.getValue().getCurrentLevel()).isEqualTo(2);
        assertThat(response.getCurrentLevel()).isEqualTo(2);
    }

    @Test
    void completeInitialStudy_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressService.completeInitialStudy(userId, resourceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(progressRepository, never()).findByUserIdAndResourceId(any(), any());
        verify(levelHistoryRepository, never()).existsByUserIdAndResourceIdAndLevel(any(), any(), any());
        verify(levelHistoryRepository, never()).save(any(LevelHistory.class));
    }

    @Test
    void completeInitialStudy_shouldThrowRuntimeExceptionWhenProgressNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressService.completeInitialStudy(userId, resourceId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Progress not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(progressRepository).findByUserIdAndResourceId(userId, resourceId);
        verify(progressRepository, never()).save(any(Progress.class));
        verify(levelHistoryRepository, never()).existsByUserIdAndResourceIdAndLevel(any(), any(), any());
        verify(levelHistoryRepository, never()).save(any(LevelHistory.class));
    }

    @Test
    void updateProgress_shouldTransitionNotStartedToInProgress() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        Progress progress = buildProgress(20L, userId, resourceId, before);

        UpdateProgressRequest request = new UpdateProgressRequest();
        request.setStatus(ProgressStatus.IN_PROGRESS);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(progressRepository.save(any(Progress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProgressResponse response = progressService.updateProgress(userId, resourceId, request);

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        Progress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(savedProgress.getStartedAt()).isNotNull();
        assertThat(savedProgress.getLastStudiedAt()).isNotNull();
        assertThat(savedProgress.getUpdatedAt()).isAfter(before);

        assertThat(response.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(response.getStartedAt()).isNotNull();
        assertThat(response.getLastStudiedAt()).isNotNull();
    }

    @Test
    void updateProgress_shouldTransitionInProgressToPaused() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        Progress progress = buildProgress(20L, userId, resourceId, before);
        progress.setStatus(ProgressStatus.IN_PROGRESS);
        progress.setStartedAt(before);
        progress.setLastStudiedAt(before);

        UpdateProgressRequest request = new UpdateProgressRequest();
        request.setStatus(ProgressStatus.PAUSED);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(progressRepository.save(any(Progress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProgressResponse response = progressService.updateProgress(userId, resourceId, request);

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        Progress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress.getStatus()).isEqualTo(ProgressStatus.PAUSED);
        assertThat(savedProgress.getLastStudiedAt()).isEqualTo(before);
        assertThat(savedProgress.getUpdatedAt()).isAfter(before);
        assertThat(response.getStatus()).isEqualTo(ProgressStatus.PAUSED);
    }

    @Test
    void updateProgress_shouldTransitionPausedToInProgress() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        Progress progress = buildProgress(20L, userId, resourceId, before);
        progress.setStatus(ProgressStatus.PAUSED);
        progress.setStartedAt(before);
        progress.setLastStudiedAt(before);

        UpdateProgressRequest request = new UpdateProgressRequest();
        request.setStatus(ProgressStatus.IN_PROGRESS);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(progressRepository.save(any(Progress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        progressService.updateProgress(userId, resourceId, request);

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        Progress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(savedProgress.getLastStudiedAt()).isNotNull();
        assertThat(savedProgress.getLastStudiedAt()).isAfter(before);
    }

    @Test
    void updateProgress_shouldArchiveProgress() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        Progress progress = buildProgress(20L, userId, resourceId, before);
        progress.setStatus(ProgressStatus.IN_PROGRESS);

        UpdateProgressRequest request = new UpdateProgressRequest();
        request.setStatus(ProgressStatus.ARCHIVED);
        request.setArchiveReason("方針変更");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(progressRepository.save(any(Progress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProgressResponse response = progressService.updateProgress(userId, resourceId, request);

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        Progress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress.getStatus()).isEqualTo(ProgressStatus.ARCHIVED);
        assertThat(savedProgress.getArchivedAt()).isNotNull();
        assertThat(savedProgress.getArchiveReason()).isEqualTo("方針変更");
        assertThat(response.getStatus()).isEqualTo(ProgressStatus.ARCHIVED);
        assertThat(response.getArchiveReason()).isEqualTo("方針変更");
    }

    @Test
    void updateProgress_shouldKeepArchiveFieldsWhenUnarchiving() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");
        Instant archivedAt = Instant.parse("2026-06-02T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        Progress progress = buildProgress(20L, userId, resourceId, before);
        progress.setStatus(ProgressStatus.ARCHIVED);
        progress.setArchivedAt(archivedAt);
        progress.setArchiveReason("方針変更");

        UpdateProgressRequest request = new UpdateProgressRequest();
        request.setStatus(ProgressStatus.IN_PROGRESS);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(progressRepository.save(any(Progress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProgressResponse response = progressService.updateProgress(userId, resourceId, request);

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        Progress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(savedProgress.getArchivedAt()).isEqualTo(archivedAt);
        assertThat(savedProgress.getArchiveReason()).isEqualTo("方針変更");
        assertThat(response.getArchivedAt()).isEqualTo(archivedAt);
        assertThat(response.getArchiveReason()).isEqualTo("方針変更");
    }

    @Test
    void updateProgress_shouldUpdateCurrentSectionIdOnly() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long sectionId = 100L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        Progress progress = buildProgress(20L, userId, resourceId, before);
        progress.setStatus(ProgressStatus.IN_PROGRESS);
        ResourceSection section = new ResourceSection();
        section.setResourceSectionId(sectionId);

        UpdateProgressRequest request = new UpdateProgressRequest();
        request.setCurrentSectionId(sectionId);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                sectionId, userId, resourceId))
                .thenReturn(Optional.of(section));
        when(progressRepository.save(any(Progress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProgressResponse response = progressService.updateProgress(userId, resourceId, request);

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        Progress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress.getCurrentSectionId()).isEqualTo(sectionId);
        assertThat(savedProgress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(response.getCurrentSectionId()).isEqualTo(sectionId);
    }

    @Test
    void updateProgress_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;

        UpdateProgressRequest request = new UpdateProgressRequest();
        request.setStatus(ProgressStatus.IN_PROGRESS);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressService.updateProgress(userId, resourceId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(progressRepository, never()).findByUserIdAndResourceId(any(), any());
        verify(progressRepository, never()).save(any(Progress.class));
    }

    @Test
    void updateProgress_shouldThrowResourceNotFoundExceptionWhenSectionNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long sectionId = 100L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        Progress progress = buildProgress(20L, userId, resourceId, before);

        UpdateProgressRequest request = new UpdateProgressRequest();
        request.setCurrentSectionId(sectionId);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                sectionId, userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressService.updateProgress(userId, resourceId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(progressRepository, never()).save(any(Progress.class));
    }

    @Test
    void updateProgress_shouldThrowInvalidProgressStatusTransitionExceptionWhenTransitionInvalid() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, before);
        Progress progress = buildProgress(20L, userId, resourceId, before);
        progress.setStatus(ProgressStatus.IN_PROGRESS);

        UpdateProgressRequest request = new UpdateProgressRequest();
        request.setStatus(ProgressStatus.NOT_STARTED);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));

        assertThatThrownBy(() -> progressService.updateProgress(userId, resourceId, request))
                .isInstanceOf(InvalidProgressStatusTransitionException.class)
                .hasMessage("Invalid progress status transition");

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
