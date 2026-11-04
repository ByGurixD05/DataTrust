package com.datatrust.application.validation;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

/**
 * Lector de filas en streaming, independiente del formato.
 *
 * <p>Cada formato (CSV, JSON, tabla relacional...) implementa esta interfaz en infraestructura.
 * Las reglas de validación solo conocen filas y nunca el formato de origen.
 */
public interface RowReader extends Closeable {

    /** Nombres de columna disponibles, normalizados con {@link TabularRow#normalize}. */
    Set<String> headers();

    /** Recorre las filas una a una sin cargarlas todas en memoria. */
    void forEachRow(RowConsumer consumer) throws IOException;

    @FunctionalInterface
    interface RowConsumer {
        void accept(TabularRow row);
    }
}
