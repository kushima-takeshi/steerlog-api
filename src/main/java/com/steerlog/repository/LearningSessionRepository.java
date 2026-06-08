package com.steerlog.repository;

import com.steerlog.entity.LearningSession;
import com.steerlog.entity.LearningSessionStatus;
import com.steerlog.entity.LearningSessionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {

    boolean existsByUserIdAndResourceIdAndSessionTypeAndStatusIn(
            Long userId,
            Long resourceId,
            LearningSessionType sessionType,
            Collection<LearningSessionStatus> statuses);

    Optional<LearningSession> findByLearningSessionIdAndUserId(
            Long learningSessionId, Long userId);
}
