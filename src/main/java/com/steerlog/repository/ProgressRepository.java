package com.steerlog.repository;

import com.steerlog.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProgressRepository extends JpaRepository<Progress, Long> {
    Optional<Progress> findByUserIdAndResourceId(Long userId, Long resourceId);
}
