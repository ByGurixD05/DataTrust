package com.datatrust.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.datatrust.domain.catalog.DatasetColumn;
import com.datatrust.domain.enums.ColumnType;

class ContentValidatorTest {

    private static final List<DatasetColumn> COLUMNS = List.of(
            column("id", 1, ColumnType.INTEGER, true),
            column("email", 2, ColumnType.STRING, true),
            column("alta", 3, ColumnType.DATE, false));

    private final ContentValidator validator = new ContentValidator();

    @Test
    void validRowsProduceValidReport() throws IOException {
        RowReader reader = reader(
                List.of("id", "email", "alta"),
                List.of(row("1", "a@x.es", "2026-01-01"), row("2", "b@x.es", "")));

        ValidationReport report = validator.validate(reader, COLUMNS);

        assertThat(report.rows()).isEqualTo(2);
        assertThat(report.valid()).isTrue();
        assertThat(report.headerErrors()).isEmpty();
        assertThat(report.samples()).isEmpty();
    }

    @Test
    void reportsWrongTypeAndEmptyRequiredCells() throws IOException {
        RowReader reader = reader(
                List.of("id", "email", "alta"),
                List.of(row("abc", "a@x.es", ""), row("2", "", "")));

        ValidationReport report = validator.validate(reader, COLUMNS);

        assertThat(report.valid()).isFalse();
        assertThat(report.errorsByColumn()).containsEntry("id", 1L).containsEntry("email", 1L);
        assertThat(report.samples()).hasSize(2);
        assertThat(report.samples().get(0).row()).isEqualTo(1);
        assertThat(report.samples().get(0).value()).isEqualTo("abc");
        assertThat(report.samples().get(0).reason()).contains("INTEGER");
    }

    @Test
    void emptyOptionalCellIsValid() throws IOException {
        RowReader reader = reader(
                List.of("id", "email", "alta"),
                List.of(row("1", "a@x.es", "")));

        assertThat(validator.validate(reader, COLUMNS).valid()).isTrue();
    }

    @Test
    void missingColumnIsHeaderErrorAndIsNotCheckedInRows() throws IOException {
        RowReader reader = reader(List.of("id"), List.of(row("1")));

        ValidationReport report = validator.validate(reader, COLUMNS);

        assertThat(report.headerErrors())
                .contains("Falta la columna 'email'", "Falta la columna 'alta'");
        assertThat(report.errorsByColumn()).doesNotContainKey("email");
        assertThat(report.valid()).isFalse();
    }

    @Test
    void columnNamesMatchIgnoringCaseAndSurroundingSpaces() throws IOException {
        List<DatasetColumn> upper = List.of(column("ID", 1, ColumnType.INTEGER, true));
        RowReader reader = reader(List.of(" id "), List.of(row("7")));

        ValidationReport report = validator.validate(reader, upper);

        assertThat(report.headerErrors()).isEmpty();
        assertThat(report.valid()).isTrue();
    }

    @Test
    void samplesAreCappedButCountersKeepCounting() throws IOException {
        List<List<String>> rows = IntStream.range(0, ContentValidator.MAX_SAMPLES + 50)
                .mapToObj(i -> row("abc", "a@x.es", ""))
                .toList();
        RowReader reader = reader(List.of("id", "email", "alta"), rows);

        ValidationReport report = validator.validate(reader, COLUMNS);

        assertThat(report.errorsByColumn()).containsEntry("id", (long) rows.size());
        assertThat(report.samples()).hasSize(ContentValidator.MAX_SAMPLES);
    }

    private static DatasetColumn column(String name, int position, ColumnType type, boolean required) {
        return DatasetColumn.restore(UUID.randomUUID(), name, position, type, required);
    }

    private static List<String> row(String... values) {
        return List.of(values);
    }

    private static RowReader reader(List<String> headers, List<List<String>> rows) {
        return new InMemoryRowReader(headers, rows);
    }

    /** Lector en memoria: prueba las reglas sin pasar por ningún formato. */
    private static final class InMemoryRowReader implements RowReader {

        private final List<String> headers;
        private final List<List<String>> rows;

        InMemoryRowReader(List<String> headers, List<List<String>> rows) {
            this.headers = headers;
            this.rows = rows;
        }

        @Override
        public Set<String> headers() {
            return headers.stream().map(TabularRow::normalize).collect(Collectors.toUnmodifiableSet());
        }

        @Override
        public void forEachRow(RowConsumer consumer) {
            for (int i = 0; i < rows.size(); i++) {
                Map<String, String> values = new HashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    values.put(TabularRow.normalize(headers.get(c)), rows.get(i).get(c));
                }
                consumer.accept(new TabularRow(i + 1L, values));
            }
        }

        @Override
        public void close() {
            // nada que cerrar
        }
    }
}
