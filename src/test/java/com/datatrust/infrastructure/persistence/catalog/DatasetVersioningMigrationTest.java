package com.datatrust.infrastructure.persistence.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import com.datatrust.support.SharedPostgres;

/** Reglas de versionado que impone la migración V3 directamente en PostgreSQL. */
@SpringBootTest
class DatasetVersioningMigrationTest {

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedPostgres.CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", SharedPostgres.CONTAINER::getUsername);
        registry.add("spring.datasource.password", SharedPostgres.CONTAINER::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate tx;

    private UUID ownerId;

    private record Created(UUID datasetId, UUID versionId) {
    }

    @BeforeEach
    void cleanAndCreateOwner() {
        jdbc.update("DELETE FROM datasets");
        jdbc.update("DELETE FROM users");
        ownerId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, username, password_hash, role, display_name, email) "
                + "VALUES (?, 'owner', 'hash', 'DATA_STEWARD', 'Owner', 'owner@example.com')", ownerId);
    }

    @Test
    void versionCannotBeUpdated() {
        Created created = createDataset("ventas", "1.0.0");

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE dataset_versions SET version_number = '9.9.9' WHERE id = ?", created.versionId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("inmutable");
    }

    @Test
    void versionCannotBeDeletedWhileItsDatasetExists() {
        Created created = createDataset("ventas", "1.0.0");

        assertThatThrownBy(() -> jdbc.update(
                "DELETE FROM dataset_versions WHERE id = ?", created.versionId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("inmutable");
    }

    @Test
    void currentVersionMustBelongToTheSameDataset() {
        Created first = createDataset("ventas", "1.0.0");
        Created second = createDataset("compras", "1.0.0");

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE datasets SET current_version_id = ? WHERE id = ?",
                first.versionId(), second.datasetId()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void versionNumberMustBeSemanticFormat() {
        assertThatThrownBy(() -> createDataset("ventas", "1.0"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void deletingDatasetRemovesItsVersions() {
        Created created = createDataset("ventas", "1.0.0");

        jdbc.update("DELETE FROM datasets WHERE id = ?", created.datasetId());

        Integer remaining = jdbc.queryForObject(
                "SELECT count(*) FROM dataset_versions WHERE dataset_id = ?", Integer.class, created.datasetId());
        assertThat(remaining).isZero();
    }

    @Test
    void newVersionCanBecomeCurrentInTheSameTransaction() {
        Created created = createDataset("ventas", "1.0.0");
        UUID newVersion = UUID.randomUUID();

        tx.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO dataset_versions (id, dataset_id, version_number, schema_snapshot, created_by) "
                    + "VALUES (?, ?, '1.1.0', '{}'::jsonb, ?)", newVersion, created.datasetId(), ownerId);
            jdbc.update("UPDATE datasets SET current_version_id = ? WHERE id = ?",
                    newVersion, created.datasetId());
        });

        String current = jdbc.queryForObject(
                "SELECT current_version_id::text FROM datasets WHERE id = ?", String.class, created.datasetId());
        assertThat(current).isEqualTo(newVersion.toString());
    }

    private Created createDataset(String name, String versionNumber) {
        UUID datasetId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        return tx.execute(status -> {
            jdbc.update("INSERT INTO datasets (id, name, description, owner_id, current_version_id) "
                    + "VALUES (?, ?, 'descripción', ?, ?)", datasetId, name, ownerId, versionId);
            jdbc.update("INSERT INTO dataset_versions (id, dataset_id, version_number, schema_snapshot, created_by) "
                    + "VALUES (?, ?, ?, '{}'::jsonb, ?)", versionId, datasetId, versionNumber, ownerId);
            return new Created(datasetId, versionId);
        });
    }
}
