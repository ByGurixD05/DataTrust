package com.datatrust.infrastructure.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.datatrust.application.validation.RowReader;
import com.datatrust.application.validation.RowReaderFactory;
import com.datatrust.domain.enums.DatasetFormat;

class FormatRowReaderFactoryTest {

    @Test
    void csvFormatOpensCsvReader() throws IOException {
        RowReaderFactory factory = new FormatRowReaderFactory();

        try (RowReader reader = factory.open(DatasetFormat.CSV,
                new ByteArrayInputStream("id,email\n1,a@x.es\n".getBytes(StandardCharsets.UTF_8)))) {
            assertThat(reader).isInstanceOf(CsvRowReader.class);
            assertThat(reader.headers()).containsExactlyInAnyOrder("id", "email");
        }
    }
}
