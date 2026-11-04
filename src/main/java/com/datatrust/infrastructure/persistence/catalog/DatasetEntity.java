package com.datatrust.infrastructure.persistence.catalog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "datasets")
public class DatasetEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "current_version_id", nullable = false)
    private UUID currentVersionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @OneToMany(mappedBy = "dataset", cascade = CascadeType.PERSIST)
    private List<DatasetVersionEntity> versions = new ArrayList<>();

    @Version
    @Column(name = "lock_version", nullable = false)
    private Integer lockVersion;

    protected DatasetEntity() {
    }

    public DatasetEntity(UUID id, String name, String description, UUID ownerId,
                         UUID currentVersionId, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.currentVersionId = currentVersionId;
        this.createdAt = createdAt;
    }

    public void addVersion(DatasetVersionEntity version) {
        versions.add(version);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getCurrentVersionId() {
        return currentVersionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
