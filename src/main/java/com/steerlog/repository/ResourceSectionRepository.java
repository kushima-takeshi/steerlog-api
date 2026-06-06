package com.steerlog.repository;

import com.steerlog.entity.ResourceSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResourceSectionRepository extends JpaRepository<ResourceSection, Long> {

    List<ResourceSection> findByUserIdAndResourceIdAndDeletedAtIsNullOrderBySectionOrderAsc(
            Long userId, Long resourceId);

    Optional<ResourceSection> findByResourceSectionIdAndUserIdAndResourceIdAndDeletedAtIsNull(
            Long resourceSectionId, Long userId, Long resourceId);
}
