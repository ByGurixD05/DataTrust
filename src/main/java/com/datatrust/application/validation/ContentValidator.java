package com.datatrust.application.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

import com.datatrust.application.validation.ValidationReport.CellError;
import com.datatrust.domain.catalog.DatasetColumn;


@Component
public class ContentValidator {

    /** Máximo de incidencias concretas en el informe; los contadores siguen contando todas. */
    static final int MAX_SAMPLES = 100;

    public ValidationReport validate(RowReader reader, List<DatasetColumn> columns)
            throws IOException {
        Set<String> headers = reader.headers();
        List<String> headerErrors = new ArrayList<>();
        List<ColumnToValidate> present = new ArrayList<>();
        for (DatasetColumn column : columns) {
            String normalizedName = TabularRow.normalize(column.name());
            if (headers.contains(normalizedName)) {
                present.add(new ColumnToValidate(column, normalizedName));
            } else {
                headerErrors.add("Falta la columna '" + column.name() + "'");
            }
        }

        ReportBuilder report = new ReportBuilder(present);
        reader.forEachRow(report::addRow);
        return report.build(headerErrors);
    }

    /** Motivo por el que el valor no cumple la columna, o {@code null} si lo cumple. */
    static String reasonFor(DatasetColumn column, String value) {
        if (value.isBlank()) {
            return column.required() ? "valor obligatorio vacío" : null;
        }
        return column.expectedType().accepts(value)
                ? null
                : "no es un valor válido de tipo " + column.expectedType();
    }

    private record ColumnToValidate(DatasetColumn column, String normalizedName) {
    }

    private static final class ReportBuilder {

        private final List<ColumnToValidate> columns;
        private final Map<String, Long> errorsByColumn = new TreeMap<>();
        private final List<CellError> samples = new ArrayList<>();
        private long rows;

        ReportBuilder(List<ColumnToValidate> columns) {
            this.columns = columns;
        }

        void addRow(TabularRow row) {
            rows++;
            for (ColumnToValidate column : columns) {
                checkCell(row, column);
            }
        }

        ValidationReport build(List<String> headerErrors) {
            return new ValidationReport(rows, headerErrors, errorsByColumn, samples);
        }

        private void checkCell(TabularRow row, ColumnToValidate checkedColumn) {
            DatasetColumn column = checkedColumn.column();
            String value = row.valueNormalized(checkedColumn.normalizedName());
            String reason = reasonFor(column, value);
            if (reason == null) {
                return;
            }
            errorsByColumn.merge(column.name(), 1L, Long::sum);
            if (samples.size() < MAX_SAMPLES) {
                samples.add(new CellError(row.number(), column.name(), value, reason));
            }
        }
    }
}
