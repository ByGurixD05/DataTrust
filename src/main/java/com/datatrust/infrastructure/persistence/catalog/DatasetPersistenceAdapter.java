package com.datatrust.infrastructure.persistence.catalog;

import com.datatrust.application.ports.DatasetRepository;
import com.datatrust.domain.catalog.Dataset;
import com.datatrust.domain.catalog.DatasetColumn;
import com.datatrust.domain.catalog.DatasetSchemaSnapshot;
import com.datatrust.domain.catalog.DatasetVersion;
import com.datatrust.domain.catalog.DatasetWithCurrentVersion;
import com.datatrust.domain.catalog.DuplicateDatasetNameException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DatasetPersistenceAdapter implements DatasetRepository {

    private static final String NAME_INDEX = "ux_datasets_name_lower";

    private final DatasetJpaRepository jpa;
    private final DatasetVersionJpaRepository versions;

    public DatasetPersistenceAdapter(DatasetJpaRepository jpa,
                                     DatasetVersionJpaRepository versions) {
        this.jpa = jpa;
        this.versions = versions;
    }

    @Override
    @Transactional
    public void save(DatasetWithCurrentVersion catalogEntry) {
        DatasetEntity datasetEntity = toEntity(catalogEntry);
        try {
            jpa.saveAndFlush(datasetEntity);
        } catch (DataIntegrityViolationException ex) {
            if (causedByNameIndex(ex)) {
                throw new DuplicateDatasetNameException(catalogEntry.dataset().name());
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DatasetWithCurrentVersion> findById(UUID id) {
        return jpa.findById(id)
                .map(entity -> toDomain(entity, loadCurrentVersion(entity)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatasetWithCurrentVersion> findPage(int page, int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "createdAt", "id"));
        List<DatasetEntity> datasets = jpa.findAll(pageable).getContent();
        return toDomain(datasets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatasetWithCurrentVersion> findPageByOwnerId(UUID ownerId, int page, int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "createdAt", "id"));
        List<DatasetEntity> datasets = jpa.findAllByOwnerId(ownerId, pageable).getContent();
        return toDomain(datasets);
    }

    private List<DatasetWithCurrentVersion> toDomain(List<DatasetEntity> datasets) {
        Map<UUID, DatasetVersionEntity> currentVersions = versions.findAllByIdIn(
                        datasets.stream().map(DatasetEntity::getCurrentVersionId).toList())
                .stream()
                .collect(Collectors.toMap(DatasetVersionEntity::getId, v -> v));
        return datasets.stream()
                .map(entity -> toDomain(entity, currentVersions.get(entity.getCurrentVersionId())))
                .toList();
    }

    private DatasetEntity toEntity(DatasetWithCurrentVersion catalogEntry) {
        Dataset dataset = catalogEntry.dataset();
        DatasetVersion version = catalogEntry.currentVersion();

        DatasetEntity entity = new DatasetEntity(
                dataset.id(),
                dataset.name(),
                dataset.description(),
                dataset.ownerId(),
                dataset.currentVersionId(),
                version.createdAt());
        DatasetVersionEntity versionEntity = new DatasetVersionEntity(
                version.id(),
                entity,
                version.versionNumber(),
                toEntity(version.schemaSnapshot()),
                version.createdAt(),
                version.createdBy());
        entity.addVersion(versionEntity);
        return entity;
    }

    private DatasetVersionEntity loadCurrentVersion(DatasetEntity entity) {
        return versions.findById(entity.getCurrentVersionId()).orElse(null);
    }

    private DatasetWithCurrentVersion toDomain(DatasetEntity entity,
                                               DatasetVersionEntity versionEntity) {
        if (versionEntity == null) {
            throw new IllegalStateException(
                    "No existe la versión vigente " + entity.getCurrentVersionId()
                            + " del dataset " + entity.getId());
        }
        DatasetVersion version = DatasetVersion.restore(
                versionEntity.getId(),
                versionEntity.getDatasetId(),
                versionEntity.getVersionNumber(),
                toDomain(versionEntity.getSchemaSnapshot()),
                versionEntity.getCreatedAt(),
                versionEntity.getCreatedBy());

        Dataset dataset = Dataset.restore(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getOwnerId(),
                entity.getCurrentVersionId());
        return new DatasetWithCurrentVersion(dataset, version);
    }

    private static DatasetVersionEntity.SchemaSnapshotEntity toEntity(
            DatasetSchemaSnapshot snapshot) {
        List<DatasetVersionEntity.ColumnSnapshotEntity> columns = snapshot.columns().stream()
                .map(column -> new DatasetVersionEntity.ColumnSnapshotEntity(
                        column.id(),
                        column.name(),
                        column.position(),
                        column.expectedType(),
                        column.required()))
                .toList();
        return new DatasetVersionEntity.SchemaSnapshotEntity(snapshot.format(), columns);
    }

    private static DatasetSchemaSnapshot toDomain(
            DatasetVersionEntity.SchemaSnapshotEntity snapshot) {
        List<DatasetColumn> columns = snapshot.columns().stream()
                .map(column -> DatasetColumn.restore(
                        column.id(),
                        column.name(),
                        column.position(),
                        column.expectedType(),
                        column.required()))
                .toList();
        return new DatasetSchemaSnapshot(snapshot.format(), columns);
    }

    private static boolean causedByNameIndex(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && message.contains(NAME_INDEX)) {
                return true;
            }
        }
        return false;
    }
}
