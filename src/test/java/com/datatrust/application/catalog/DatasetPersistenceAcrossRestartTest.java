package com.datatrust.application.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.datatrust.domain.catalog.DatasetColumn;
import com.datatrust.domain.enums.ColumnType;
import com.datatrust.domain.enums.DatasetFormat;
import com.datatrust.application.auth.catalog.DatasetService;
import com.datatrust.application.ports.DatasetRepository;
import com.datatrust.support.SharedPostgres;
import com.datatrust.support.StandaloneApplication;

class DatasetPersistenceAcrossRestartTest {

    @Test
    void datasetSurvivesApplicationRestart() {
        UUID id;
        UUID ownerId = UUID.randomUUID();

        try (ConfigurableApplicationContext first =
                     StandaloneApplication.start(SharedPostgres.CONTAINER)) {
            first.getBean(JdbcTemplate.class).update("""
                    INSERT INTO users
                        (id, username, password_hash, role, display_name, email)
                    VALUES (?, 'restart-owner', 'test-hash', 'DATA_STEWARD',
                            'Restart owner', 'restart-owner@example.com')
                    """, ownerId);
            id = first.getBean(DatasetService.class).register(
                    "persistente", "sobrevive al reinicio", ownerId,
                    DatasetFormat.CSV, 1, 0, 0,
                    List.of(DatasetColumn.create("id", 1, ColumnType.INTEGER, true)));
        }

        try (ConfigurableApplicationContext second =
                     StandaloneApplication.start(SharedPostgres.CONTAINER)) {
            var found = second.getBean(DatasetRepository.class).findById(id).orElseThrow();

            assertThat(found.dataset().name()).isEqualTo("persistente");
            assertThat(found.currentVersion().schemaSnapshot().columns()).hasSize(1);
            assertThat(found.currentVersion().versionNumber()).isEqualTo("1.0.0");
        }
    }
}