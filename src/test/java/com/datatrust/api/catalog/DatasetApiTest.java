package com.datatrust.api.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.datatrust.support.SharedPostgres;

@SpringBootTest
@AutoConfigureMockMvc
class DatasetApiTest {

    private static final String BASE = "/api/v1/datasets";

    private static final String ID_AND_EMAIL = """
            [
              {"name": "id", "position": 1, "expectedType": "INTEGER", "required": true},
              {"name": "email", "position": 2, "expectedType": "STRING", "required": true}
            ]
            """;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedPostgres.CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", SharedPostgres.CONTAINER::getUsername);
        registry.add("spring.datasource.password", SharedPostgres.CONTAINER::getPassword);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String authorization;

    @BeforeEach
    void cleanDatabaseAndAuthenticateOwner() throws Exception {
        // Las versiones se borran en cascada por la clave foránea
        jdbc.update("DELETE FROM datasets");
        jdbc.update("DELETE FROM users");
        MvcResult result = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "dataset-owner",
                                  "password": "dataset-test-password-123",
                                  "displayName": "Dataset owner",
                                  "email": "dataset-owner@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        authorization = "Bearer " + jsonValue(
                result.getResponse().getContentAsString(), "accessToken");
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        MvcResult result = mvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datasetJson("clientes", ID_AND_EMAIL)))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .matches("http://localhost/api/v1/datasets/[0-9a-f\\-]{36}");
    }

    @Test
    void getReturnsColumnsOrderedByPosition() throws Exception {
        String columnsReversed = """
                [
                  {"name": "email", "position": 2, "expectedType": "STRING", "required": true},
                  {"name": "id", "position": 1, "expectedType": "INTEGER", "required": true}
                ]
                """;
        String id = createAndGetId(datasetJson("clientes", columnsReversed));

        mvc.perform(get(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("clientes"))
                .andExpect(jsonPath("$.columns[0].name").value("id"))
                .andExpect(jsonPath("$.columns[1].name").value("email"));
    }

    @Test
    void dataStewardCannotReadAnotherOwnersDataset() throws Exception {
        String otherOwner = registerAndGetAuthorization("other-owner");
        String otherDataset = createAndGetId(
                datasetJson("otro-dataset", ID_AND_EMAIL), otherOwner);

        mvc.perform(get(BASE + "/" + otherDataset)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingDatasetStillReturns404ForAnotherOwner() throws Exception {
        String otherOwner = registerAndGetAuthorization("missing-owner");

        mvc.perform(get(BASE + "/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, otherOwner))
                .andExpect(status().isNotFound());
    }

    @Test
    void dataStewardListContainsOnlyOwnedDatasets() throws Exception {
        String otherOwner = registerAndGetAuthorization("list-other-owner");
        createAndGetId(datasetJson("propio", ID_AND_EMAIL));
        createAndGetId(datasetJson("ajeno", ID_AND_EMAIL), otherOwner);

        mvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("propio"));
    }

    @Test
    void adminCanReadAndListDatasetsFromAllOwners() throws Exception {
        String otherOwner = registerAndGetAuthorization("admin-other-owner");
        String otherDataset = createAndGetId(
                datasetJson("dataset-ajeno", ID_AND_EMAIL), otherOwner);
        String adminAuthorization = createAdminAndGetAuthorization();

        mvc.perform(get(BASE + "/" + otherDataset)
                        .header(HttpHeaders.AUTHORIZATION, adminAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("dataset-ajeno"));

        mvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, adminAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void duplicateNameIgnoringCaseReturns409() throws Exception {
        createAndGetId(datasetJson("clientes", ID_AND_EMAIL));

        mvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datasetJson("CLIENTES", ID_AND_EMAIL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflictingName").value("CLIENTES"));
    }

    @Test
    void missingColumnsReturns400WithFieldErrors() throws Exception {
        mvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datasetJson("clientes", "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.columns").exists());
    }

    @Test
    void duplicateColumnNamesReturn400WithCode() throws Exception {
        String duplicated = """
                [
                  {"name": "id", "position": 1, "expectedType": "INTEGER", "required": true},
                  {"name": "ID", "position": 2, "expectedType": "INTEGER", "required": true}
                ]
                """;

        mvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datasetJson("clientes", duplicated)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COLUMN_NAME_DUPLICATED"))
                .andExpect(jsonPath("$.field").value("columns.name"));
    }

    @Test
    void unknownIdReturns404() throws Exception {
        String id = UUID.randomUUID().toString();

        mvc.perform(get(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.datasetId").value(id));
    }

    @Test
    void listRespectsPageAndSizeLimit() throws Exception {
        createAndGetId(datasetJson("uno", ID_AND_EMAIL));
        createAndGetId(datasetJson("dos", ID_AND_EMAIL));
        createAndGetId(datasetJson("tres", ID_AND_EMAIL));

        mvc.perform(get(BASE).param("page", "0").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get(BASE).param("page", "1").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get(BASE).param("size", "500")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isBadRequest());
    }

    private String createAndGetId(String json) throws Exception {
        return createAndGetId(json, authorization);
    }

    private String createAndGetId(String json, String ownerAuthorization) throws Exception {
        MvcResult result = mvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, ownerAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private String registerAndGetAuthorization(String username) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "dataset-test-password-123",
                                  "displayName": "Other owner",
                                  "email": "%s@example.com"
                                }
                                """.formatted(username, username)))
                .andExpect(status().isCreated())
                .andReturn();
        return "Bearer " + jsonValue(result.getResponse().getContentAsString(), "accessToken");
    }

    private String createAdminAndGetAuthorization() throws Exception {
        String passwordHash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE username = 'dataset-owner'", String.class);
        jdbc.update("""
                INSERT INTO users (id, username, password_hash, role, display_name, email)
                VALUES (?, 'dataset-admin', ?, 'ADMIN', 'Dataset admin', 'dataset-admin@example.com')
                """, UUID.randomUUID(), passwordHash);
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"dataset-admin","password":"dataset-test-password-123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + jsonValue(result.getResponse().getContentAsString(), "accessToken");
    }

    private static String datasetJson(String name, String columns) {
        return """
                {
                  "name": "%s",
                  "description": "Prueba de integración",
                  "format": "CSV",
                  "major": 1,
                  "minor": 2,
                  "patch": 3,
                  "columns": %s
                }
                """.formatted(name, columns);
    }

    private static String jsonValue(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json);
        assertThat(matcher.find()).as("JSON field %s", field).isTrue();
        return matcher.group(1);
    }
}