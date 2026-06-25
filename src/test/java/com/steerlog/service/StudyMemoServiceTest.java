package com.steerlog.service;

import com.steerlog.dto.request.CreateStudyMemoRequest;
import com.steerlog.dto.request.UpdateStudyMemoRequest;
import com.steerlog.dto.response.StudyMemoResponse;
import com.steerlog.entity.Progress;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.entity.Resource;
import com.steerlog.entity.ResourceSection;
import com.steerlog.entity.ResourceType;
import com.steerlog.entity.StudyMemo;
import com.steerlog.entity.StudyMemoType;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.repository.ProgressRepository;
import com.steerlog.repository.ResourceRepository;
import com.steerlog.repository.ResourceSectionRepository;
import com.steerlog.repository.StudyMemoRepository;
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
class StudyMemoServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ResourceSectionRepository resourceSectionRepository;

    @Mock
    private StudyMemoRepository studyMemoRepository;

    @Mock
    private ProgressRepository progressRepository;

    @InjectMocks
    private StudyMemoService studyMemoService;

    @Test
    void createMemo_shouldCreateMemoAndUpdateProgress() {
        Long userId = 1L;
        Long resourceId = 10L;

        Resource resource = buildResource(resourceId, userId);
        Progress progress = buildProgress(300L, userId, resourceId);

        CreateStudyMemoRequest request = new CreateStudyMemoRequest();
        request.setContent("HTTPの基本を理解した");
        request.setMemoType(StudyMemoType.LEARNED);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(studyMemoRepository.save(any(StudyMemo.class))).thenAnswer(invocation -> {
            StudyMemo memo = invocation.getArgument(0);
            memo.setStudyMemoId(500L);
            return memo;
        });
        when(progressRepository.save(any(Progress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudyMemoResponse response = studyMemoService.createMemo(userId, resourceId, request);

        ArgumentCaptor<StudyMemo> memoCaptor = ArgumentCaptor.forClass(StudyMemo.class);
        verify(studyMemoRepository).save(memoCaptor.capture());

        StudyMemo savedMemo = memoCaptor.getValue();
        assertThat(savedMemo.getUserId()).isEqualTo(userId);
        assertThat(savedMemo.getResourceId()).isEqualTo(resourceId);
        assertThat(savedMemo.getResourceSectionId()).isNull();
        assertThat(savedMemo.getMemoType()).isEqualTo(StudyMemoType.LEARNED);
        assertThat(savedMemo.getContent()).isEqualTo("HTTPの基本を理解した");
        assertThat(savedMemo.getDeletedAt()).isNull();
        assertThat(savedMemo.getCreatedAt()).isNotNull();
        assertThat(savedMemo.getUpdatedAt()).isNotNull();

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        Progress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(savedProgress.getLastStudiedAt()).isNotNull();
        assertThat(savedProgress.getUpdatedAt()).isNotNull();
        assertThat(savedProgress.getCurrentLevel()).isEqualTo(0);

        assertThat(response.getStudyMemoId()).isEqualTo(500L);
        assertThat(response.getResourceId()).isEqualTo(resourceId);
        assertThat(response.getMemoType()).isEqualTo(StudyMemoType.LEARNED);
        assertThat(response.getContent()).isEqualTo("HTTPの基本を理解した");
    }

    @Test
    void createMemo_shouldNotChangeProgressStatusWhenAlreadyInProgress() {
        Long userId = 1L;
        Long resourceId = 10L;

        Resource resource = buildResource(resourceId, userId);
        Progress progress = buildProgress(300L, userId, resourceId);
        progress.setStatus(ProgressStatus.IN_PROGRESS);

        CreateStudyMemoRequest request = new CreateStudyMemoRequest();
        request.setContent("メモ内容");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(progress));
        when(studyMemoRepository.save(any(StudyMemo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(progressRepository.save(any(Progress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studyMemoService.createMemo(userId, resourceId, request);

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        Progress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(savedProgress.getLastStudiedAt()).isNotNull();
        assertThat(savedProgress.getUpdatedAt()).isNotNull();
    }

    @Test
    void createMemo_shouldUseGeneralWhenMemoTypeIsNull() {
        Long userId = 1L;
        Long resourceId = 10L;

        Resource resource = buildResource(resourceId, userId);

        CreateStudyMemoRequest request = new CreateStudyMemoRequest();
        request.setContent("メモ内容");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(buildProgress(300L, userId, resourceId)));
        when(studyMemoRepository.save(any(StudyMemo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(progressRepository.save(any(Progress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studyMemoService.createMemo(userId, resourceId, request);

        ArgumentCaptor<StudyMemo> memoCaptor = ArgumentCaptor.forClass(StudyMemo.class);
        verify(studyMemoRepository).save(memoCaptor.capture());

        assertThat(memoCaptor.getValue().getMemoType()).isEqualTo(StudyMemoType.GENERAL);
    }

    @Test
    void createMemo_shouldValidateSectionWhenResourceSectionIdProvided() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;

        Resource resource = buildResource(resourceId, userId);
        ResourceSection section = buildSection(resourceSectionId, userId, resourceId);

        CreateStudyMemoRequest request = new CreateStudyMemoRequest();
        request.setResourceSectionId(resourceSectionId);
        request.setContent("第1章のメモ");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.of(section));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.of(buildProgress(300L, userId, resourceId)));
        when(studyMemoRepository.save(any(StudyMemo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(progressRepository.save(any(Progress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studyMemoService.createMemo(userId, resourceId, request);

        verify(resourceSectionRepository).findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId);

        ArgumentCaptor<StudyMemo> memoCaptor = ArgumentCaptor.forClass(StudyMemo.class);
        verify(studyMemoRepository).save(memoCaptor.capture());

        assertThat(memoCaptor.getValue().getResourceSectionId()).isEqualTo(resourceSectionId);
    }

    @Test
    void createMemo_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;

        CreateStudyMemoRequest request = new CreateStudyMemoRequest();
        request.setContent("メモ内容");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyMemoService.createMemo(userId, resourceId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(progressRepository, never()).findByUserIdAndResourceId(any(), any());
        verify(studyMemoRepository, never()).save(any(StudyMemo.class));
    }

    @Test
    void createMemo_shouldThrowRuntimeExceptionWhenProgressNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;

        Resource resource = buildResource(resourceId, userId);

        CreateStudyMemoRequest request = new CreateStudyMemoRequest();
        request.setContent("メモ内容");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId(userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyMemoService.createMemo(userId, resourceId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Progress not found");

        verify(progressRepository).findByUserIdAndResourceId(userId, resourceId);
        verify(studyMemoRepository, never()).save(any(StudyMemo.class));
        verify(progressRepository, never()).save(any(Progress.class));
    }

    @Test
    void createMemo_shouldThrowResourceNotFoundExceptionWhenSectionNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;

        Resource resource = buildResource(resourceId, userId);

        CreateStudyMemoRequest request = new CreateStudyMemoRequest();
        request.setResourceSectionId(resourceSectionId);
        request.setContent("第1章のメモ");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyMemoService.createMemo(userId, resourceId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceSectionRepository).findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId);
        verify(progressRepository, never()).findByUserIdAndResourceId(any(), any());
        verify(studyMemoRepository, never()).save(any(StudyMemo.class));
    }

    @Test
    void getMemos_shouldReturnMemos() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        Resource resource = buildResource(resourceId, userId);
        StudyMemo memo1 = buildMemo(500L, userId, resourceId, null, StudyMemoType.LEARNED, "メモ1", now);
        StudyMemo memo2 = buildMemo(501L, userId, resourceId, null, StudyMemoType.QUESTION, "メモ2", now);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(studyMemoRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, resourceId))
                .thenReturn(List.of(memo1, memo2));

        List<StudyMemoResponse> responses = studyMemoService.getMemos(userId, resourceId);

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(studyMemoRepository).findByUserIdAndResourceIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, resourceId);
        verify(progressRepository, never()).findByUserIdAndResourceId(any(), any());

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getStudyMemoId()).isEqualTo(500L);
        assertThat(responses.get(0).getMemoType()).isEqualTo(StudyMemoType.LEARNED);
        assertThat(responses.get(0).getContent()).isEqualTo("メモ1");
        assertThat(responses.get(1).getStudyMemoId()).isEqualTo(501L);
        assertThat(responses.get(1).getMemoType()).isEqualTo(StudyMemoType.QUESTION);
        assertThat(responses.get(1).getContent()).isEqualTo("メモ2");
    }

    @Test
    void getMemos_shouldReturnEmptyListWhenNoMemos() {
        Long userId = 1L;
        Long resourceId = 10L;

        Resource resource = buildResource(resourceId, userId);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(studyMemoRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, resourceId))
                .thenReturn(Collections.emptyList());

        List<StudyMemoResponse> responses = studyMemoService.getMemos(userId, resourceId);

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(studyMemoRepository).findByUserIdAndResourceIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, resourceId);
        verify(progressRepository, never()).findByUserIdAndResourceId(any(), any());

        assertThat(responses).isEmpty();
    }

    @Test
    void getMemos_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyMemoService.getMemos(userId, resourceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(studyMemoRepository, never())
                .findByUserIdAndResourceIdAndDeletedAtIsNullOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void getMemo_shouldReturnMemo() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long studyMemoId = 500L;
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        Resource resource = buildResource(resourceId, userId);
        StudyMemo memo = buildMemo(studyMemoId, userId, resourceId, null, StudyMemoType.LEARNED, "HTTPの基本を理解した", now);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(studyMemoRepository.findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                studyMemoId, userId, resourceId))
                .thenReturn(Optional.of(memo));

        StudyMemoResponse response = studyMemoService.getMemo(userId, resourceId, studyMemoId);

        assertThat(response.getStudyMemoId()).isEqualTo(studyMemoId);
        assertThat(response.getResourceId()).isEqualTo(resourceId);
        assertThat(response.getMemoType()).isEqualTo(StudyMemoType.LEARNED);
        assertThat(response.getContent()).isEqualTo("HTTPの基本を理解した");
        assertThat(response.getCreatedAt()).isEqualTo(now);
        assertThat(response.getUpdatedAt()).isEqualTo(now);

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(studyMemoRepository).findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                studyMemoId, userId, resourceId);
    }

    @Test
    void getMemo_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long studyMemoId = 500L;

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyMemoService.getMemo(userId, resourceId, studyMemoId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(studyMemoRepository, never())
                .findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(any(), any(), any());
    }

    @Test
    void getMemo_shouldThrowResourceNotFoundExceptionWhenMemoNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long studyMemoId = 500L;

        Resource resource = buildResource(resourceId, userId);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(studyMemoRepository.findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                studyMemoId, userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyMemoService.getMemo(userId, resourceId, studyMemoId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(studyMemoRepository).findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                studyMemoId, userId, resourceId);
    }

    @Test
    void updateMemo_shouldUpdateOnlyProvidedFields() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long studyMemoId = 500L;
        Instant before = Instant.parse("2026-06-03T10:00:00Z");

        Resource resource = buildResource(resourceId, userId);
        StudyMemo memo = buildMemo(studyMemoId, userId, resourceId, null, StudyMemoType.GENERAL, "旧メモ", before);

        UpdateStudyMemoRequest request = new UpdateStudyMemoRequest();
        request.setContent("新メモ");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(studyMemoRepository.findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                studyMemoId, userId, resourceId))
                .thenReturn(Optional.of(memo));
        when(studyMemoRepository.save(any(StudyMemo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudyMemoResponse response = studyMemoService.updateMemo(userId, resourceId, studyMemoId, request);

        ArgumentCaptor<StudyMemo> memoCaptor = ArgumentCaptor.forClass(StudyMemo.class);
        verify(studyMemoRepository).save(memoCaptor.capture());

        StudyMemo savedMemo = memoCaptor.getValue();
        assertThat(savedMemo.getContent()).isEqualTo("新メモ");
        assertThat(savedMemo.getMemoType()).isEqualTo(StudyMemoType.GENERAL);
        assertThat(savedMemo.getUpdatedAt()).isNotNull();
        assertThat(savedMemo.getUpdatedAt()).isAfter(before);

        assertThat(response.getStudyMemoId()).isEqualTo(studyMemoId);
        assertThat(response.getContent()).isEqualTo("新メモ");
        assertThat(response.getMemoType()).isEqualTo(StudyMemoType.GENERAL);
    }

    @Test
    void updateMemo_shouldUpdateMemoType() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long studyMemoId = 500L;
        Instant before = Instant.parse("2026-06-03T10:00:00Z");

        Resource resource = buildResource(resourceId, userId);
        StudyMemo memo = buildMemo(studyMemoId, userId, resourceId, null, StudyMemoType.GENERAL, "メモ内容", before);

        UpdateStudyMemoRequest request = new UpdateStudyMemoRequest();
        request.setMemoType(StudyMemoType.QUESTION);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(studyMemoRepository.findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                studyMemoId, userId, resourceId))
                .thenReturn(Optional.of(memo));
        when(studyMemoRepository.save(any(StudyMemo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studyMemoService.updateMemo(userId, resourceId, studyMemoId, request);

        ArgumentCaptor<StudyMemo> memoCaptor = ArgumentCaptor.forClass(StudyMemo.class);
        verify(studyMemoRepository).save(memoCaptor.capture());

        assertThat(memoCaptor.getValue().getMemoType()).isEqualTo(StudyMemoType.QUESTION);
        assertThat(memoCaptor.getValue().getContent()).isEqualTo("メモ内容");
    }

    @Test
    void updateMemo_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long studyMemoId = 500L;

        UpdateStudyMemoRequest request = new UpdateStudyMemoRequest();
        request.setContent("新メモ");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyMemoService.updateMemo(userId, resourceId, studyMemoId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(studyMemoRepository, never())
                .findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(any(), any(), any());
        verify(studyMemoRepository, never()).save(any(StudyMemo.class));
    }

    @Test
    void updateMemo_shouldThrowResourceNotFoundExceptionWhenMemoNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long studyMemoId = 500L;

        Resource resource = buildResource(resourceId, userId);

        UpdateStudyMemoRequest request = new UpdateStudyMemoRequest();
        request.setContent("新メモ");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(studyMemoRepository.findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                studyMemoId, userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyMemoService.updateMemo(userId, resourceId, studyMemoId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(studyMemoRepository).findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                studyMemoId, userId, resourceId);
        verify(studyMemoRepository, never()).save(any(StudyMemo.class));
    }

    @Test
    void deleteMemo_shouldSoftDeleteMemo() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long studyMemoId = 500L;
        Instant before = Instant.parse("2026-06-03T10:00:00Z");

        Resource resource = buildResource(resourceId, userId);
        StudyMemo memo = buildMemo(studyMemoId, userId, resourceId, null, StudyMemoType.GENERAL, "メモ内容", before);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(studyMemoRepository.findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                studyMemoId, userId, resourceId))
                .thenReturn(Optional.of(memo));
        when(studyMemoRepository.save(any(StudyMemo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studyMemoService.deleteMemo(userId, resourceId, studyMemoId);

        ArgumentCaptor<StudyMemo> memoCaptor = ArgumentCaptor.forClass(StudyMemo.class);
        verify(studyMemoRepository).save(memoCaptor.capture());

        StudyMemo savedMemo = memoCaptor.getValue();
        assertThat(savedMemo.getDeletedAt()).isNotNull();
        assertThat(savedMemo.getUpdatedAt()).isNotNull();
    }

    @Test
    void deleteMemo_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long studyMemoId = 500L;

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyMemoService.deleteMemo(userId, resourceId, studyMemoId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(studyMemoRepository, never())
                .findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(any(), any(), any());
        verify(studyMemoRepository, never()).save(any(StudyMemo.class));
    }

    @Test
    void deleteMemo_shouldThrowResourceNotFoundExceptionWhenMemoNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long studyMemoId = 500L;

        Resource resource = buildResource(resourceId, userId);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(studyMemoRepository.findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                studyMemoId, userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyMemoService.deleteMemo(userId, resourceId, studyMemoId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(studyMemoRepository).findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                studyMemoId, userId, resourceId);
        verify(studyMemoRepository, never()).save(any(StudyMemo.class));
    }

    private Resource buildResource(Long resourceId, Long userId) {
        Instant now = Instant.parse("2026-06-01T10:00:00Z");
        Resource resource = new Resource();
        resource.setResourceId(resourceId);
        resource.setUserId(userId);
        resource.setResourceType(ResourceType.BOOK);
        resource.setTitle("Webを支える技術");
        resource.setCreatedAt(now);
        resource.setUpdatedAt(now);
        return resource;
    }

    private ResourceSection buildSection(Long resourceSectionId, Long userId, Long resourceId) {
        Instant now = Instant.parse("2026-06-03T10:00:00Z");
        ResourceSection section = new ResourceSection();
        section.setResourceSectionId(resourceSectionId);
        section.setUserId(userId);
        section.setResourceId(resourceId);
        section.setTitle("第1章");
        section.setSectionOrder(1);
        section.setCreatedAt(now);
        section.setUpdatedAt(now);
        return section;
    }

    private Progress buildProgress(Long progressId, Long userId, Long resourceId) {
        Instant now = Instant.parse("2026-06-01T10:00:00Z");
        Progress progress = new Progress();
        progress.setProgressId(progressId);
        progress.setUserId(userId);
        progress.setResourceId(resourceId);
        progress.setStatus(ProgressStatus.NOT_STARTED);
        progress.setCurrentLevel(0);
        progress.setCreatedAt(now);
        progress.setUpdatedAt(now);
        return progress;
    }

    private StudyMemo buildMemo(
            Long studyMemoId,
            Long userId,
            Long resourceId,
            Long resourceSectionId,
            StudyMemoType memoType,
            String content,
            Instant createdAt) {
        StudyMemo memo = new StudyMemo();
        memo.setStudyMemoId(studyMemoId);
        memo.setUserId(userId);
        memo.setResourceId(resourceId);
        memo.setResourceSectionId(resourceSectionId);
        memo.setMemoType(memoType);
        memo.setContent(content);
        memo.setCreatedAt(createdAt);
        memo.setUpdatedAt(createdAt);
        return memo;
    }
}
