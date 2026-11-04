package com.datatrust.application.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


import com.datatrust.domain.catalog.DatasetWithCurrentVersion;

public interface DatasetRepository {
    void save(DatasetWithCurrentVersion dataset);
    Optional<DatasetWithCurrentVersion> findById(UUID id);
    List<DatasetWithCurrentVersion> findPage(int page, int size);
    List<DatasetWithCurrentVersion> findPageByOwnerId(UUID ownerId, int page, int size);
}