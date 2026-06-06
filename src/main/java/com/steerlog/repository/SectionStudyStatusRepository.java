package com.steerlog.repository;

import com.steerlog.entity.SectionStudyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionStudyStatusRepository extends JpaRepository<SectionStudyStatus, Long> {

    List<SectionStudyStatus> findByUserIdAndResourceId(Long userId, Long resourceId);

    Optional<SectionStudyStatus> findByUserIdAndResourceSectionId(Long userId, Long resourceSectionId);

    boolean existsByUserIdAndResourceSectionId(Long userId, Long resourceSectionId);
}
