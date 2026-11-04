package com.datatrust.infrastructure.persistence.catalog;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetJpaRepository extends JpaRepository<DatasetEntity, UUID> {
    Page<DatasetEntity> findAllByOwnerId(UUID ownerId, Pageable pageable);
}