package com.steerlog.service;

import com.steerlog.dto.request.CreateStudyMemoRequest;
import com.steerlog.dto.response.StudyMemoResponse;
import com.steerlog.entity.Resource;
import com.steerlog.entity.ResourceSection;
import com.steerlog.entity.ResourceType;
import com.steerlog.entity.StudyMemo;
import com.steerlog.entity.StudyMemoType;
import com.steerlog.exception.ResourceNotFoundException;
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

    @InjectMocks
    private StudyMemoService studyMemoService;

    @Test
    void createMemo_shouldCreateMemo() {
        Long userId = 1L;
        Long resourceId = 10L;

        Resource resource = buildResource(resourceId, userId);

        CreateStudyMemoRequest request = new CreateStudyMemoRequest();
        request.setContent("HTTPの基本を理解した");
        request.setMemoType(StudyMemoType.LEARNED);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(studyMemoRepository.save(any(StudyMemo.class))).thenAnswer(invocation -> {
            StudyMemo memo = invocation.getArgument(0);
            memo.setStudyMemoId(500L);
            return memo;
        });

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

        assertThat(response.getStudyMemoId()).isEqualTo(500L);
        assertThat(response.getResourceId()).isEqualTo(resourceId);
        assertThat(response.getMemoType()).isEqualTo(StudyMemoType.LEARNED);
        assertThat(response.getContent()).isEqualTo("HTTPの基本を理解した");
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
        when(studyMemoRepository.save(any(StudyMemo.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
        when(studyMemoRepository.save(any(StudyMemo.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
        verify(studyMemoRepository, never()).save(any(StudyMemo.class));
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
