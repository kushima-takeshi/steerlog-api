package com.steerlog.service;

import com.steerlog.dto.request.CreateResourceRequest;
import com.steerlog.dto.response.CreateResourceResponse;
import com.steerlog.dto.response.ProgressResponse;
import com.steerlog.dto.response.ResourceDetailResponse;
import com.steerlog.dto.response.ResourceListItemResponse;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.entity.Progress;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.entity.Resource;
import com.steerlog.repository.ProgressRepository;
import com.steerlog.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ProgressRepository progressRepository;

    public ResourceService(ResourceRepository resourceRepository, ProgressRepository progressRepository) {
        this.resourceRepository = resourceRepository;
        this.progressRepository = progressRepository;
    }

    @Transactional
    public CreateResourceResponse createResource(Long userId, CreateResourceRequest request) {
        Instant now = Instant.now();

        Resource resource = new Resource();
        resource.setUserId(userId);
        resource.setResourceType(request.getResourceType());
        resource.setTitle(request.getTitle());
        resource.setAuthor(request.getAuthor());
        resource.setSourceUrl(request.getSourceUrl());
        resource.setDescription(request.getDescription());
        resource.setCreatedAt(now);
        resource.setUpdatedAt(now);

        Resource savedResource = resourceRepository.save(resource);

        Progress progress = new Progress();
        progress.setUserId(userId);
        progress.setResourceId(savedResource.getResourceId());
        progress.setStatus(ProgressStatus.NOT_STARTED);
        progress.setCurrentLevel(0);
        progress.setCreatedAt(now);
        progress.setUpdatedAt(now);

        Progress savedProgress = progressRepository.save(progress);

        return toCreateResourceResponse(savedResource, savedProgress);
    }

    @Transactional(readOnly = true)
    public ResourceDetailResponse getResourceDetail(Long userId, Long resourceId) {
        Resource resource = resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        Progress progress = progressRepository
                .findByUserIdAndResourceId(userId, resourceId)
                .orElseThrow(() -> new RuntimeException("Progress not found"));

        return toResourceDetailResponse(resource, progress);
    }

    @Transactional(readOnly = true)
    public List<ResourceListItemResponse> getResources(Long userId) {
        List<Resource> resources = resourceRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        if (resources.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> resourceIds = resources.stream()
                .map(Resource::getResourceId)
                .toList();

        List<Progress> progresses = progressRepository.findByUserIdAndResourceIdIn(userId, resourceIds);
        Map<Long, Progress> progressByResourceId = progresses.stream()
                .collect(Collectors.toMap(Progress::getResourceId, progress -> progress));

        List<ResourceListItemResponse> responses = new ArrayList<>();
        for (Resource resource : resources) {
            Progress progress = progressByResourceId.get(resource.getResourceId());
            if (progress == null) {
                throw new RuntimeException("Progress not found for resourceId=" + resource.getResourceId());
            }
            responses.add(toResourceListItemResponse(resource, progress));
        }
        return responses;
    }

    @Transactional
    public void deleteResource(Long userId, Long resourceId) {
        Resource resource = resourceRepository
                .findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        Instant now = Instant.now();
        resource.setDeletedAt(now);
        resource.setUpdatedAt(now);
        resourceRepository.save(resource);
    }

    private CreateResourceResponse toCreateResourceResponse(Resource resource, Progress progress) {
        CreateResourceResponse response = new CreateResourceResponse();
        response.setResourceId(resource.getResourceId());
        response.setResourceType(resource.getResourceType());
        response.setTitle(resource.getTitle());
        response.setAuthor(resource.getAuthor());
        response.setSourceUrl(resource.getSourceUrl());
        response.setDescription(resource.getDescription());
        response.setCreatedAt(resource.getCreatedAt());
        response.setUpdatedAt(resource.getUpdatedAt());
        response.setProgress(toProgressResponse(progress));
        return response;
    }

    private ProgressResponse toProgressResponse(Progress progress) {
        ProgressResponse response = new ProgressResponse();
        response.setProgressId(progress.getProgressId());
        response.setStatus(progress.getStatus());
        response.setCurrentLevel(progress.getCurrentLevel());
        response.setCurrentSectionId(progress.getCurrentSectionId());
        response.setStartedAt(progress.getStartedAt());
        response.setCompletedAt(progress.getCompletedAt());
        response.setArchivedAt(progress.getArchivedAt());
        response.setArchiveReason(progress.getArchiveReason());
        response.setInitialStudiedAt(progress.getInitialStudiedAt());
        response.setLastStudiedAt(progress.getLastStudiedAt());
        response.setCreatedAt(progress.getCreatedAt());
        response.setUpdatedAt(progress.getUpdatedAt());
        return response;
    }

    private ResourceDetailResponse toResourceDetailResponse(Resource resource, Progress progress) {
        ResourceDetailResponse response = new ResourceDetailResponse();
        response.setResourceId(resource.getResourceId());
        response.setResourceType(resource.getResourceType());
        response.setTitle(resource.getTitle());
        response.setAuthor(resource.getAuthor());
        response.setSourceUrl(resource.getSourceUrl());
        response.setDescription(resource.getDescription());
        response.setCreatedAt(resource.getCreatedAt());
        response.setUpdatedAt(resource.getUpdatedAt());
        response.setProgress(toProgressResponse(progress));
        return response;
    }

    private ResourceListItemResponse toResourceListItemResponse(Resource resource, Progress progress) {
        ResourceListItemResponse response = new ResourceListItemResponse();
        response.setResourceId(resource.getResourceId());
        response.setResourceType(resource.getResourceType());
        response.setTitle(resource.getTitle());
        response.setAuthor(resource.getAuthor());
        response.setSourceUrl(resource.getSourceUrl());
        response.setDescription(resource.getDescription());
        response.setCreatedAt(resource.getCreatedAt());
        response.setUpdatedAt(resource.getUpdatedAt());
        response.setProgress(toProgressResponse(progress));
        return response;
    }
}
