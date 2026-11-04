package com.datatrust.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ColumnTypeTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "INTEGER | 42",
            "INTEGER | '  -7  '",
            "DECIMAL | 4.20",
            "DECIMAL | -0.5",
            "BOOLEAN | TRUE",
            "BOOLEAN | false",
            "DATE | 2026-10-21",
            "STRING | cualquier cosa"
    })
    void acceptsValidValues(ColumnType type, String value) {
        assertThat(type.accepts(value)).isTrue();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "INTEGER | 4.2",
            "INTEGER | abc",
            "DECIMAL | 1,5",
            "DECIMAL | x",
            "BOOLEAN | yes",
            "DATE | 21/10/2026",
            "DATE | 2026-02-30"
    })
    void rejectsInvalidValues(ColumnType type, String value) {
        assertThat(type.accepts(value)).isFalse();
    }

    @Test
    void floatIsReplacedByExactDecimal() {
        assertThatThrownBy(() -> ColumnType.valueOf("FLOAT"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(ColumnType.valueOf("DECIMAL")).isNotNull();
    }
}
