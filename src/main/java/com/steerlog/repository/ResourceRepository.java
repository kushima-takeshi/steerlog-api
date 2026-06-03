package com.steerlog.repository;

import com.steerlog.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Optional<Resource> findByResourceIdAndUserIdAndDeletedAtIsNull(Long resourceId, Long userId);
}
