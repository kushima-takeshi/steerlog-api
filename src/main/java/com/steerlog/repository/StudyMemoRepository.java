package com.steerlog.repository;

import com.steerlog.entity.StudyMemo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyMemoRepository extends JpaRepository<StudyMemo, Long> {

    List<StudyMemo> findByUserIdAndResourceIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long userId, Long resourceId);

    Optional<StudyMemo> findByStudyMemoIdAndUserIdAndResourceIdAndDeletedAtIsNull(
            Long studyMemoId, Long userId, Long resourceId);
}
