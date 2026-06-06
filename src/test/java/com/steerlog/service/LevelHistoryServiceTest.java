package com.steerlog.service;

import com.steerlog.dto.response.LevelHistoryResponse;
import com.steerlog.entity.LevelHistory;
import com.steerlog.entity.LevelHistoryReasonCode;
import com.steerlog.entity.LevelHistorySourceType;
import com.steerlog.entity.Resource;
import com.steerlog.entity.ResourceType;
import com.steerlog.exception.ResourceNotFoundException;
import com.steerlog.repository.LevelHistoryRepository;
import com.steerlog.repository.ResourceRepository;
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
class LevelHistoryServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private LevelHistoryRepository levelHistoryRepository;

    @InjectMocks
    private LevelHistoryService levelHistoryService;

    @Test
    void getLevelHistories_shouldReturnLevelHistories() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant earlier = Instant.parse("2026-06-01T10:00:00Z");
        Instant later = Instant.parse("2026-06-03T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, earlier);
        LevelHistory levelHistory1 = buildLevelHistory(100L, userId, resourceId, 1, earlier);
        LevelHistory levelHistory2 = buildLevelHistory(200L, userId, resourceId, 2, later);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(levelHistoryRepository.findByUserIdAndResourceIdOrderByCreatedAtAsc(userId, resourceId))
                .thenReturn(List.of(levelHistory1, levelHistory2));

        List<LevelHistoryResponse> responses = levelHistoryService.getLevelHistories(userId, resourceId);

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(levelHistoryRepository).findByUserIdAndResourceIdOrderByCreatedAtAsc(userId, resourceId);

        assertThat(responses).hasSize(2);

        assertThat(responses.get(0).getLevelHistoryId()).isEqualTo(100L);
        assertThat(responses.get(0).getLevel()).isEqualTo(1);
        assertThat(responses.get(0).getSourceType()).isEqualTo(LevelHistorySourceType.INITIAL_STUDY_COMPLETION);
        assertThat(responses.get(0).getSourceId()).isNull();
        assertThat(responses.get(0).getReasonCode()).isEqualTo(LevelHistoryReasonCode.INITIAL_STUDY_COMPLETED);
        assertThat(responses.get(0).getCreatedAt()).isEqualTo(earlier);

        assertThat(responses.get(1).getLevelHistoryId()).isEqualTo(200L);
        assertThat(responses.get(1).getLevel()).isEqualTo(2);
        assertThat(responses.get(1).getSourceType()).isEqualTo(LevelHistorySourceType.LEARNING_SESSION_RECORD);
        assertThat(responses.get(1).getSourceId()).isEqualTo(50L);
        assertThat(responses.get(1).getReasonCode()).isEqualTo(LevelHistoryReasonCode.IMMEDIATE_REFLECTION_RECORD_SAVED);
        assertThat(responses.get(1).getCreatedAt()).isEqualTo(later);
    }

    @Test
    void getLevelHistories_shouldReturnEmptyListWhenNoHistories() {
        Long userId = 1L;
        Long resourceId = 10L;
        Instant now = Instant.parse("2026-06-01T10:00:00Z");

        Resource resource = buildResource(resourceId, userId, now);

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.of(resource));
        when(levelHistoryRepository.findByUserIdAndResourceIdOrderByCreatedAtAsc(userId, resourceId))
                .thenReturn(Collections.emptyList());

        List<LevelHistoryResponse> responses = levelHistoryService.getLevelHistories(userId, resourceId);

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(levelHistoryRepository).findByUserIdAndResourceIdOrderByCreatedAtAsc(userId, resourceId);

        assertThat(responses).isEmpty();
    }

    @Test
    void getLevelHistories_shouldThrowResourceNotFoundExceptionWhenResourceNotFound() {
        Long userId = 1L;
        Long resourceId = 10L;

        when(resourceRepository.findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> levelHistoryService.getLevelHistories(userId, resourceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource not found");

        verify(resourceRepository).findByResourceIdAndUserIdAndDeletedAtIsNull(resourceId, userId);
        verify(levelHistoryRepository, never()).findByUserIdAndResourceIdOrderByCreatedAtAsc(userId, resourceId);
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

    private LevelHistory buildLevelHistory(
            Long levelHistoryId,
            Long userId,
            Long resourceId,
            Integer level,
            Instant createdAt) {
        LevelHistory levelHistory = new LevelHistory();
        levelHistory.setLevelHistoryId(levelHistoryId);
        levelHistory.setUserId(userId);
        levelHistory.setResourceId(resourceId);
        levelHistory.setLevel(level);
        levelHistory.setCreatedAt(createdAt);

        if (level == 1) {
            levelHistory.setSourceType(LevelHistorySourceType.INITIAL_STUDY_COMPLETION);
            levelHistory.setSourceId(null);
            levelHistory.setReasonCode(LevelHistoryReasonCode.INITIAL_STUDY_COMPLETED);
        } else {
            levelHistory.setSourceType(LevelHistorySourceType.LEARNING_SESSION_RECORD);
            levelHistory.setSourceId(50L);
            levelHistory.setReasonCode(LevelHistoryReasonCode.IMMEDIATE_REFLECTION_RECORD_SAVED);
        }

        return levelHistory;
    }
}
