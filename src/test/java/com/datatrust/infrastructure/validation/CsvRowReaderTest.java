package com.datatrust.infrastructure.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.datatrust.application.validation.MalformedContentException;
import com.datatrust.application.validation.TabularRow;

class CsvRowReaderTest {

    @Test
    void headersAreNormalized() throws IOException {
        try (CsvRowReader reader = csv("Alta, ID ,EMAIL\n")) {
            assertThat(reader.headers()).containsExactlyInAnyOrder("alta", "id", "email");
        }
    }

    @Test
    void deliversEveryRowWithItsValues() throws IOException {
        try (CsvRowReader reader = csv("id,email\n1,a@x.es\n2,b@x.es\n")) {
            List<TabularRow> rows = collect(reader);

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0).value("email")).isEqualTo("a@x.es");
            assertThat(rows.get(1).value("id")).isEqualTo("2");
        }
    }

    @Test
    void shortRowGivesEmptyValueForMissingColumns() throws IOException {
        try (CsvRowReader reader = csv("id,email\n1\n")) {
            List<TabularRow> rows = collect(reader);

            assertThat(rows.get(0).value("email")).isEmpty();
        }
    }

    @Test
    void quotedValueKeepsItsComma() throws IOException {
        try (CsvRowReader reader = csv("id,name\n1,\"Pérez, Ana\"\n")) {
            assertThat(collect(reader).get(0).value("name")).isEqualTo("Pérez, Ana");
        }
    }

    @Test
    void unclosedQuoteInDataIsMalformedContent() throws IOException {
        try (CsvRowReader reader = csv("id,name\n1,\"sin cerrar\n")) {
            assertThatThrownBy(() -> reader.forEachRow(row -> { }))
                    .isInstanceOf(MalformedContentException.class);
        }
    }

    @Test
    void unclosedQuoteInHeaderIsMalformedContent() {
        assertThatThrownBy(() -> csv("\"id,email\n1,a\n"))
                .isInstanceOf(MalformedContentException.class);
    }

    private static CsvRowReader csv(String text) throws IOException {
        return new CsvRowReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static List<TabularRow> collect(CsvRowReader reader) throws IOException {
        List<TabularRow> rows = new ArrayList<>();
        reader.forEachRow(rows::add);
        return rows;
    }
}
