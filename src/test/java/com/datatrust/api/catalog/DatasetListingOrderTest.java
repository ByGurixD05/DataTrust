package com.datatrust.api.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/** El listado sale en orden de creación, estable entre páginas. */
@SpringBootTest
@AutoConfigureMockMvc
class DatasetListingOrderTest {

    private static final String BASE = "/api/v1/datasets";

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
    void setUp() throws Exception {
        jdbc.update("DELETE FROM datasets");
        jdbc.update("DELETE FROM users");
        MvcResult result = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "lister",
                                  "password": "listing-test-password-123",
                                  "displayName": "Lister",
                                  "email": "lister@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        authorization = "Bearer " + jsonValue(result.getResponse().getContentAsString(), "accessToken");
    }

    @Test
    void listIsOrderedByCreationTime() throws Exception {
        create("primero");
        create("segundo");
        create("tercero");

        mvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("primero"))
                .andExpect(jsonPath("$[1].name").value("segundo"))
                .andExpect(jsonPath("$[2].name").value("tercero"));
    }

    @Test
    void pagesDoNotOverlapAndKeepTheOrder() throws Exception {
        create("primero");
        create("segundo");
        create("tercero");

        mvc.perform(get(BASE).param("page", "1").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("tercero"));
    }

    private void create(String name) throws Exception {
        String body = """
                {
                  "name": "%s",
                  "description": "Orden de listado",
                  "format": "CSV",
                  "major": 1,
                  "minor": 0,
                  "patch": 0,
                  "columns": [
                    {"name": "id", "position": 1, "expectedType": "INTEGER", "required": true}
                  ]
                }
                """.formatted(name);
        mvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private static String jsonValue(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json);
        matcher.find();
        return matcher.group(1);
    }
}
