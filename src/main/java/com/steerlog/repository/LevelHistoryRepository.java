package com.steerlog.repository;

import com.steerlog.entity.LevelHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelHistoryRepository extends JpaRepository<LevelHistory, Long> {

    boolean existsByUserIdAndResourceIdAndLevel(Long userId, Long resourceId, Integer level);
}
