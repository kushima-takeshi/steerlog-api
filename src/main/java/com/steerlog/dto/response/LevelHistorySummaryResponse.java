package com.steerlog.dto.response;

import com.steerlog.entity.LevelHistoryReasonCode;
import com.steerlog.entity.LevelHistorySourceType;

import java.time.Instant;

public class LevelHistorySummaryResponse {

    private Integer level;
    private LevelHistorySourceType sourceType;
    private Long sourceId;
    private LevelHistoryReasonCode reasonCode;
    private Instant createdAt;

    public LevelHistorySummaryResponse() {
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
