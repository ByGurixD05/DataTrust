package com.datatrust.api.auth;

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
class AuthApiTest {

    private static final String DATASETS = "/api/v1/datasets";

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

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM datasets");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void registrationCreatesDataStewardAndTokenProvidesDatasetOwner() throws Exception {
        String username = "register-" + UUID.randomUUID();
        String email = username + "@example.com";
        MvcResult registration = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username, email, ",\"role\":\"ADMIN\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        String tokenResponse = registration.getResponse().getContentAsString();
        UUID userId = UUID.fromString(jsonValue(tokenResponse, "userId"));
        String token = jsonValue(tokenResponse, "accessToken");
        String storedHash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?", String.class, userId);
        String storedRole = jdbc.queryForObject(
                "SELECT role FROM users WHERE id = ?", String.class, userId);

        assertThat(storedHash).isNotEqualTo("test-password-123");
        assertThat(storedRole).isEqualTo("DATA_STEWARD");

        mvc.perform(post(DATASETS)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datasetJson()))
                .andExpect(status().isCreated());

        UUID ownerId = jdbc.queryForObject(
                "SELECT owner_id FROM datasets WHERE name = 'auth-owned'", UUID.class);
        assertThat(ownerId).isEqualTo(userId);
    }

    @Test
    void loginIsCaseInsensitiveAndInvalidCredentialsAreUnauthorized() throws Exception {
        String username = "login-" + UUID.randomUUID();
        String email = username + "@example.com";
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username, email, "")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"test-password-123"}
                                """.formatted(username.toUpperCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"incorrect-password"}
                                """.formatted(username)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedResourcesRejectMissingAndInvalidTokens() throws Exception {
        mvc.perform(get(DATASETS))
                .andExpect(status().isUnauthorized());

        mvc.perform(get(DATASETS)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationReportsDuplicateUsernameAndEmail() throws Exception {
        String username = "duplicate-" + UUID.randomUUID();
        String email = username + "@example.com";
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username, email, "")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username.toUpperCase(),
                                "different-" + email, "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.field").value("username"));

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("different-" + username, email.toUpperCase(), "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.field").value("email"));
    }

    @Test
    void registrationRejectsShortPasswords() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "short-password",
                                  "password": "short",
                                  "displayName": "Short password",
                                  "email": "short-password@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private static String registerJson(String username, String email, String extra) {
        return """
                {
                  "username": "%s",
                  "password": "test-password-123",
                  "displayName": "Authentication test",
                  "email": "%s"%s
                }
                """.formatted(username, email, extra);
    }

    private static String jsonValue(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json);
        assertThat(matcher.find()).as("JSON field %s", field).isTrue();
        return matcher.group(1);
    }

    private static String datasetJson() {
        return """
                {
                  "name": "auth-owned",
                  "description": "Created with a bearer token",
                  "format": "CSV",
                  "major": 1,
                  "minor": 0,
                  "patch": 0,
                  "columns": [
                    {"name": "id", "position": 1, "expectedType": "INTEGER", "required": true}
                  ]
                }
                """;
    }
}
