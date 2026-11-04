package com.datatrust.infrastructure.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.datatrust.application.validation.RowReader;
import com.datatrust.application.validation.RowReaderFactory;
import com.datatrust.domain.enums.DatasetFormat;

/**
 * Fábrica de lectores por formato. Para añadir un formato: crear su {@link RowReader}
 * y registrarlo en el constructor. Nada más cambia.
 */
@Component
public final class FormatRowReaderFactory implements RowReaderFactory {

    private final Map<DatasetFormat, RowReaderOpener> openers = new EnumMap<>(DatasetFormat.class);

    public FormatRowReaderFactory() {
        openers.put(DatasetFormat.CSV, CsvRowReader::new);
    }

    @Override
    public RowReader open(DatasetFormat format, InputStream content) throws IOException {
        RowReaderOpener opener = openers.get(format);
        if (opener == null) {
            throw new IllegalArgumentException("Formato no soportado: " + format);
        }
        return opener.open(content);
    }

    @FunctionalInterface
    private interface RowReaderOpener {
        RowReader open(InputStream content) throws IOException;
    }
}
