package com.datatrust.application.validation;

import java.util.List;
import java.util.Map;

/**
 * Resultado de validar un contenido en streaming.
 *
 * @param rows           filas de datos leídas (sin cabecera)
 * @param headerErrors   problemas de cabecera, como columnas esperadas que faltan
 * @param errorsByColumn número de celdas incorrectas por columna
 * @param samples        primeras incidencias concretas, acotadas para no crecer con el contenido
 */
public record ValidationReport(
        long rows,
        List<String> headerErrors,
        Map<String, Long> errorsByColumn,
        List<CellError> samples) {

    /** Una celda concreta que no cumple la columna esperada. */
    public record CellError(long row, String column, String value, String reason) {
    }

    public boolean valid() {
        return headerErrors.isEmpty() && errorsByColumn.isEmpty();
    }
}
