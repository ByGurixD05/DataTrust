package com.datatrust.application.validation;

import java.io.IOException;
import java.io.InputStream;

import com.datatrust.domain.enums.DatasetFormat;

/**
 * Crea el {@link RowReader} adecuado para un formato.
 *
 * <p>Es el punto de extensión: para soportar un formato nuevo se implementa su lector
 * en infraestructura y se registra en la fábrica concreta. Las reglas no cambian.
 */
public interface RowReaderFactory {

    RowReader open(DatasetFormat format, InputStream content) throws IOException;
}
