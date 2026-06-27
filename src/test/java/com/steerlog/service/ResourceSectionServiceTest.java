package com.steerlog.service;

import com.steerlog.dto.request.CreateResourceSectionRequest;
import com.steerlog.dto.request.UpdateResourceSectionRequest;
import com.steerlog.dto.response.ResourceSectionResponse;
import com.steerlog.entity.Resource;
import com.steerlog.entity.ResourceSection;
import com.steerlog.entity.ResourceType;
import com.steerlog.entity.SectionStudyStatus;
import com.steerlog.exception.ResourceNotFoundException;
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
class ResourceSectionServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ResourceSectionRepository resourceSectionRepository;

    @Mock
    private SectionStudyStatusRepository sectionStudyStatusRepository;

    @InjectMocks
    private ResourceSectionService resourceSectionService;

    @Test
    void createSection_shouldCreateSectionAndInitialStudyStatus() {
        Long userId = 1L;
        Long resourceId = 10L;

        Resource resource = buildResource(resourceId, userId);

        CreateResourceSectionRequest request = new CreateResourceSectionRequest();
        request.setTitle("第1章");
        request.setSectionOrder(1);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.save(any(ResourceSection.class))).thenAnswer(invocation -> {
            ResourceSection section = invocation.getArgument(0);
            section.setResourceSectionId(100L);
            return section;
        });
        when(sectionStudyStatusRepository.save(any(SectionStudyStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResourceSectionResponse response = resourceSectionService.createSection(userId, resourceId, request);

        ArgumentCaptor<ResourceSection> sectionCaptor = ArgumentCaptor.forClass(ResourceSection.class);
        verify(resourceSectionRepository).save(sectionCaptor.capture());

        ResourceSection savedSection = sectionCaptor.getValue();
        assertThat(savedSection.getUserId()).isEqualTo(userId);
        assertThat(savedSection.getResourceId()).isEqualTo(resourceId);
        assertThat(savedSection.getTitle()).isEqualTo("第1章");
        assertThat(savedSection.getSectionOrder()).isEqualTo(1);
        assertThat(savedSection.getCreatedAt()).isNotNull();
        assertThat(savedSection.getUpdatedAt()).isNotNull();

        ArgumentCaptor<SectionStudyStatus> statusCaptor = ArgumentCaptor.forClass(SectionStudyStatus.class);
        verify(sectionStudyStatusRepository).save(statusCaptor.capture());

        SectionStudyStatus savedStatus = statusCaptor.getValue();
        assertThat(savedStatus.getUserId()).isEqualTo(userId);
        assertThat(savedStatus.getResourceId()).isEqualTo(resourceId);
        assertThat(savedStatus.getResourceSectionId()).isEqualTo(100L);
        assertThat(savedStatus.getStudiedAt()).isNull();
        assertThat(savedStatus.getCreatedAt()).isNotNull();
        assertThat(savedStatus.getUpdatedAt()).isNotNull();

        assertThat(response.getResourceSectionId()).isEqualTo(100L);
        assertThat(response.getResourceId()).isEqualTo(resourceId);
        assertThat(response.getTitle()).isEqualTo("第1章");
        assertThat(response.getSectionOrder()).isEqualTo(1);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void createSection_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;

        CreateResourceSectionRequest request = new CreateResourceSectionRequest();
        request.setTitle("第1章");
        request.setSectionOrder(1);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceSectionService.createSection(userId, resourceId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(resourceSectionRepository, never()).save(any(ResourceSection.class));
        verify(sectionStudyStatusRepository, never()).save(any(SectionStudyStatus.class));
    }

    @Test
    void getSections_shouldReturnSections() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-03T10:00:00Z");

        Resource resource = buildResource(resourceId, userId);
        ResourceSection section1 = buildSection(100L, userId, resourceId, "第1章", 1, now);
        ResourceSection section2 = buildSection(200L, userId, resourceId, "第2章", 2, now);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(
                userId, resourceId))
                .thenReturn(List.of(section1, section2));

        List<ResourceSectionResponse> responses = resourceSectionService.getSections(userId, resourceId);

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(resourceSectionRepository)
                .findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(userId, resourceId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getResourceSectionId()).isEqualTo(100L);
        assertThat(responses.get(0).getTitle()).isEqualTo("第1章");
        assertThat(responses.get(0).getSectionOrder()).isEqualTo(1);
        assertThat(responses.get(1).getResourceSectionId()).isEqualTo(200L);
        assertThat(responses.get(1).getTitle()).isEqualTo("第2章");
        assertThat(responses.get(1).getSectionOrder()).isEqualTo(2);
    }

    @Test
    void getSections_shouldReturnEmptyListWhenNoSections() {
        Long userId = 1L;
        Long resourceId = 10L;

        Resource resource = buildResource(resourceId, userId);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(
                userId, resourceId))
                .thenReturn(Collections.emptyList());

        List<ResourceSectionResponse> responses = resourceSectionService.getSections(userId, resourceId);

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(resourceSectionRepository)
                .findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(userId, resourceId);

        assertThat(responses).isEmpty();
    }

    @Test
    void getSections_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceSectionService.getSections(userId, resourceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(resourceSectionRepository, never())
                .findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(any(), any());
    }

    @Test
    void updateSection_shouldUpdateSection() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId);
        ResourceSection section = buildSection(resourceSectionId, userId, resourceId, "第1章", 1, before);

        UpdateResourceSectionRequest request = new UpdateResourceSectionRequest();
        request.setTitle("更新後タイトル");
        request.setSectionOrder(2);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.of(section));
        when(resourceSectionRepository.save(any(ResourceSection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResourceSectionResponse response =
                resourceSectionService.updateSection(userId, resourceId, resourceSectionId, request);

        ArgumentCaptor<ResourceSection> sectionCaptor = ArgumentCaptor.forClass(ResourceSection.class);
        verify(resourceSectionRepository).save(sectionCaptor.capture());

        ResourceSection savedSection = sectionCaptor.getValue();
        assertThat(savedSection.getTitle()).isEqualTo("更新後タイトル");
        assertThat(savedSection.getSectionOrder()).isEqualTo(2);
        assertThat(savedSection.getUserId()).isEqualTo(userId);
        assertThat(savedSection.getResourceId()).isEqualTo(resourceId);
        assertThat(savedSection.getCreatedAt()).isEqualTo(before);
        assertThat(savedSection.getUpdatedAt()).isNotNull();
        assertThat(savedSection.getUpdatedAt()).isAfter(before);

        assertThat(response.getResourceSectionId()).isEqualTo(resourceSectionId);
        assertThat(response.getResourceId()).isEqualTo(resourceId);
        assertThat(response.getTitle()).isEqualTo("更新後タイトル");
        assertThat(response.getSectionOrder()).isEqualTo(2);

        verify(sectionStudyStatusRepository, never()).save(any(SectionStudyStatus.class));
    }

    @Test
    void updateSection_shouldUpdateOnlyProvidedFields() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;
        Instant before = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId);
        ResourceSection section = buildSection(resourceSectionId, userId, resourceId, "第1章", 1, before);

        UpdateResourceSectionRequest request = new UpdateResourceSectionRequest();
        request.setTitle("更新後タイトル");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.of(section));
        when(resourceSectionRepository.save(any(ResourceSection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResourceSectionResponse response =
                resourceSectionService.updateSection(userId, resourceId, resourceSectionId, request);

        ArgumentCaptor<ResourceSection> sectionCaptor = ArgumentCaptor.forClass(ResourceSection.class);
        verify(resourceSectionRepository).save(sectionCaptor.capture());

        ResourceSection savedSection = sectionCaptor.getValue();
        assertThat(savedSection.getTitle()).isEqualTo("更新後タイトル");
        assertThat(savedSection.getSectionOrder()).isEqualTo(1);

        assertThat(response.getTitle()).isEqualTo("更新後タイトル");
        assertThat(response.getSectionOrder()).isEqualTo(1);
    }

    @Test
    void updateSection_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;

        UpdateResourceSectionRequest request = new UpdateResourceSectionRequest();
        request.setTitle("更新後タイトル");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceSectionService.updateSection(
                        userId, resourceId, resourceSectionId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(resourceSectionRepository, never())
                .findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(any(), any(), any());
        verify(resourceSectionRepository, never()).save(any(ResourceSection.class));
    }

    @Test
    void updateSection_shouldThrowResourceNotFoundExceptionWhenSectionNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;

        Resource resource = buildResource(resourceId, userId);

        UpdateResourceSectionRequest request = new UpdateResourceSectionRequest();
        request.setTitle("更新後タイトル");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceSectionService.updateSection(
                        userId, resourceId, resourceSectionId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceSectionRepository).findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId);
        verify(resourceSectionRepository, never()).save(any(ResourceSection.class));
    }

    @Test
    void updateSection_shouldThrowResourceNotFoundExceptionWhenSectionBelongsToOtherResource() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long otherResourceId = 20L;
        Long resourceSectionId = 100L;

        Resource resource = buildResource(resourceId, userId);

        UpdateResourceSectionRequest request = new UpdateResourceSectionRequest();
        request.setTitle("更新後タイトル");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceSectionService.updateSection(
                        userId, resourceId, resourceSectionId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceSectionRepository).findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId);
        verify(resourceSectionRepository, never()).findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, otherResourceId);
        verify(resourceSectionRepository, never()).save(any(ResourceSection.class));
    }

    @Test
    void updateSection_shouldThrowResourceNotFoundExceptionWhenSectionIsDeleted() {
        Long userId = 1L;
        Long resourceId = 10L;
        Long resourceSectionId = 100L;

        Resource resource = buildResource(resourceId, userId);

        UpdateResourceSectionRequest request = new UpdateResourceSectionRequest();
        request.setTitle("更新後タイトル");

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(resourceSectionRepository.findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceSectionService.updateSection(
                        userId, resourceId, resourceSectionId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceSectionRepository).findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                resourceSectionId, userId, resourceId);
        verify(resourceSectionRepository, never()).save(any(ResourceSection.class));
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
}
