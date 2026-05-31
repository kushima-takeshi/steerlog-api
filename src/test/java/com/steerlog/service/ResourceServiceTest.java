package com.steerlog.service;

import com.steerlog.dto.request.CreateResourceRequest;
import com.steerlog.dto.response.CreateResourceResponse;
import com.steerlog.entity.Progress;
import com.steerlog.entity.ProgressStatus;
import com.steerlog.entity.Resource;
import com.steerlog.entity.ResourceType;
import com.steerlog.repository.ProgressRepository;
import com.steerlog.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ProgressRepository progressRepository;

    @InjectMocks
    private ResourceService resourceService;

    @Test
    void createResource_shouldCreateResourceAndInitialProgress() {
        Long userId = 1L;
        CreateResourceRequest request = new CreateResourceRequest();
        request.setResourceType(ResourceType.BOOK);
        request.setTitle("Webを支える技術");
        request.setAuthor("山本陽平");
        request.setSourceUrl("https://example.com");
        request.setDescription("REST/API設計の基礎を学ぶための本");

        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> {
            Resource resource = invocation.getArgument(0);
            resource.setResourceId(10L);
            return resource;
        });
        when(progressRepository.save(any(Progress.class))).thenAnswer(invocation -> {
            Progress progress = invocation.getArgument(0);
            progress.setProgressId(20L);
            return progress;
        });

        CreateResourceResponse response = resourceService.createResource(userId, request);

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceRepository).save(resourceCaptor.capture());

        ArgumentCaptor<Progress> progressCaptor = ArgumentCaptor.forClass(Progress.class);
        verify(progressRepository).save(progressCaptor.capture());

        Progress capturedProgress = progressCaptor.getValue();
        assertThat(capturedProgress.getUserId()).isEqualTo(userId);
        assertThat(capturedProgress.getResourceId()).isEqualTo(10L);
        assertThat(capturedProgress.getStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
        assertThat(capturedProgress.getCurrentLevel()).isEqualTo(0);
        assertThat(capturedProgress.getCreatedAt()).isNotNull();
        assertThat(capturedProgress.getUpdatedAt()).isNotNull();

        assertThat(response.getResourceId()).isEqualTo(10L);
        assertThat(response.getResourceType()).isEqualTo(ResourceType.BOOK);
        assertThat(response.getTitle()).isEqualTo("Webを支える技術");
        assertThat(response.getAuthor()).isEqualTo("山本陽平");
        assertThat(response.getSourceUrl()).isEqualTo("https://example.com");
        assertThat(response.getDescription()).isEqualTo("REST/API設計の基礎を学ぶための本");
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();

        assertThat(response.getProgress()).isNotNull();
        assertThat(response.getProgress().getProgressId()).isEqualTo(20L);
        assertThat(response.getProgress().getStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
        assertThat(response.getProgress().getCurrentLevel()).isEqualTo(0);
        assertThat(response.getProgress().getCreatedAt()).isNotNull();
        assertThat(response.getProgress().getUpdatedAt()).isNotNull();
    }
}
