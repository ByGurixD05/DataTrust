package com.datatrust.infrastructure.persistence.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.datatrust.support.StandaloneApplication;

class FlywayMigrationTest {

    @Test
    void appliesMigrationsOnEmptyDatabase() {
        try (PostgreSQLContainer database = new PostgreSQLContainer("postgres:16-alpine")) {
            database.start();

            try (ConfigurableApplicationContext context = StandaloneApplication.start(database)) {
                JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);

                Integer applied = jdbc.queryForObject(
                        "SELECT count(*) FROM flyway_schema_history "
                                + "WHERE version IN ('1', '2') AND success",
                        Integer.class);

                Integer tables = jdbc.queryForObject(
                        "SELECT count(*) FROM information_schema.tables "
                                + "WHERE table_schema = 'public' "
                                + "AND table_name IN ('datasets', 'dataset_versions', 'users')",
                        Integer.class);
                Integer snapshots = jdbc.queryForObject(
                        "SELECT count(*) FROM information_schema.columns "
                                + "WHERE table_schema = 'public' "
                                + "AND table_name = 'dataset_versions' "
                                + "AND column_name = 'schema_snapshot' "
                                + "AND udt_name = 'jsonb'",
                        Integer.class);
                Integer ownerForeignKey = jdbc.queryForObject(
                        "SELECT count(*) FROM information_schema.key_column_usage k "
                                + "JOIN information_schema.constraint_column_usage c "
                                + "ON c.constraint_catalog = k.constraint_catalog "
                                + "AND c.constraint_schema = k.constraint_schema "
                                + "AND c.constraint_name = k.constraint_name "
                                + "WHERE k.constraint_schema = 'public' "
                                + "AND k.table_name = 'datasets' "
                                + "AND k.column_name = 'owner_id' "
                                + "AND k.constraint_name = 'fk_datasets_owner' "
                                + "AND c.table_name = 'users' AND c.column_name = 'id'",
                        Integer.class);
                Integer userColumns = jdbc.queryForObject(
                        "SELECT count(*) FROM information_schema.columns "
                                + "WHERE table_schema = 'public' "
                                + "AND table_name = 'users' "
                                + "AND column_name IN ('id', 'username', 'password_hash', 'role', "
                                + "'display_name', 'email', 'active', 'created_at', 'updated_at')",
                        Integer.class);

                assertThat(applied).isEqualTo(2);
                assertThat(tables).isEqualTo(3);
                assertThat(snapshots).isEqualTo(1);
                assertThat(ownerForeignKey).isEqualTo(1);
                assertThat(userColumns).isEqualTo(9);
            }
        }
    }
}