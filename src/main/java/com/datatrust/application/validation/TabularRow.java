package com.datatrust.application.validation;

import java.util.Locale;
import java.util.Map;

/**
 * Una fila de datos independiente del formato de origen.
 *
 * @param number número de fila para informar de errores (1 = primera fila de datos)
 * @param values valores por nombre de columna normalizado (ver {@link #normalize})
 */
public record TabularRow(long number, Map<String, String> values) {

    public TabularRow {
        values = Map.copyOf(values);
    }

    /** Valor de la columna, o cadena vacía si la fila no la tiene. */
    public String value(String columnName) {
        return valueNormalized(normalize(columnName));
    }

    String valueNormalized(String normalizedColumnName) {
        return values.getOrDefault(normalizedColumnName, "");
    }

    /** Nombre de columna normalizado: sin espacios alrededor y en minúsculas. */
    public static String normalize(String columnName) {
        return columnName.strip().toLowerCase(Locale.ROOT);
    }
}
