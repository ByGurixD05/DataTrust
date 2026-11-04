package com.datatrust.infrastructure.persistence.catalog;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetVersionJpaRepository extends JpaRepository<DatasetVersionEntity, UUID> {

    List<DatasetVersionEntity> findAllByIdIn(Collection<UUID> ids);
    
}
