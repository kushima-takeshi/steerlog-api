package com.steerlog.service;

import com.steerlog.dto.request.CreateResourceSectionRequest;
import com.steerlog.dto.request.UpdateResourceSectionRequest;
import com.steerlog.dto.response.ResourceSectionResponse;
import com.steerlog.entity.ResourceSection;
import com.steerlog.entity.SectionStudyStatus;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.repository.ResourceRepository;
import com.steerlog.repository.ResourceSectionRepository;
import com.steerlog.repository.SectionStudyStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ResourceSectionService {

    private final ResourceRepository resourceRepository;
    private final ResourceSectionRepository resourceSectionRepository;
    private final SectionStudyStatusRepository sectionStudyStatusRepository;

    public ResourceSectionService(
            ResourceRepository resourceRepository,
            ResourceSectionRepository resourceSectionRepository,
            SectionStudyStatusRepository sectionStudyStatusRepository) {
        this.resourceRepository = resourceRepository;
        this.resourceSectionRepository = resourceSectionRepository;
        this.sectionStudyStatusRepository = sectionStudyStatusRepository;
    }

    @Transactional
    public ResourceSectionResponse createSection(
            Long userId, Long resourceId, CreateResourceSectionRequest request) {
        resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        Instant now = Instant.now();

        ResourceSection section = new ResourceSection();
        section.setUserId(userId);
        section.setResourceId(resourceId);
        section.setTitle(request.getTitle());
        section.setSectionOrder(request.getSectionOrder());
        section.setCreatedAt(now);
        section.setUpdatedAt(now);

        ResourceSection savedSection = resourceSectionRepository.save(section);

        SectionStudyStatus studyStatus = new SectionStudyStatus();
        studyStatus.setUserId(userId);
        studyStatus.setResourceId(resourceId);
        studyStatus.setResourceSectionId(savedSection.getResourceSectionId());
        studyStatus.setStudiedAt(null);
        studyStatus.setCreatedAt(now);
        studyStatus.setUpdatedAt(now);

        sectionStudyStatusRepository.save(studyStatus);

        return toResourceSectionResponse(savedSection);
    }

    @Transactional
    public ResourceSectionResponse updateSection(
            Long userId, Long resourceId, Long resourceSectionId, UpdateResourceSectionRequest request) {
        resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        ResourceSection section = resourceSectionRepository
                .findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
                        resourceSectionId, userId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        if (request.getTitle() != null) {
            section.setTitle(request.getTitle());
        }
        if (request.getSectionOrder() != null) {
            section.setSectionOrder(request.getSectionOrder());
        }

        section.setUpdatedAt(Instant.now());
        ResourceSection savedSection = resourceSectionRepository.save(section);

        return toResourceSectionResponse(savedSection);
    }

    @Transactional(readOnly = true)
    public List<ResourceSectionResponse> getSections(Long userId, Long resourceId) {
        resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        List<ResourceSection> sections = resourceSectionRepository
                .findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(userId, resourceId);

        return sections.stream()
                .map(this::toResourceSectionResponse)
                .toList();
    }

    private ResourceSectionResponse toResourceSectionResponse(ResourceSection section) {
        ResourceSectionResponse response = new ResourceSectionResponse();
        response.setResourceSectionId(section.getResourceSectionId());
        response.setResourceId(section.getResourceId());
        response.setTitle(section.getTitle());
        response.setSectionOrder(section.getSectionOrder());
        response.setCreatedAt(section.getCreatedAt());
        response.setUpdatedAt(section.getUpdatedAt());
        return response;
    }
}
