package com.steerlog.repository;

import com.steerlog.entity.LearningSessionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LearningSessionRecordRepository extends JpaRepository<LearningSessionRecord, Long> {

    boolean existsByLearningSessionIdAndUserIdAndResourceId(
            Long learningSessionId, Long userId, Long resourceId);

    Optional<LearningSessionRecord> findByLearningSessionRecordIdAndUserIdAndResourceId(
            Long learningSessionRecordId, Long userId, Long resourceId);
}
