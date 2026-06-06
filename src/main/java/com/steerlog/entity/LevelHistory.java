package com.steerlog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "level_histories")
public class LevelHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "level_history_id")
    private Long levelHistoryId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private LevelHistorySourceType sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 100)
    private LevelHistoryReasonCode reasonCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public LevelHistory() {
    }

    public Long getLevelHistoryId() {
        return levelHistoryId;
    }

    public void setLevelHistoryId(Long levelHistoryId) {
        this.levelHistoryId = levelHistoryId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public LevelHistorySourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(LevelHistorySourceType sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public LevelHistoryReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(LevelHistoryReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
