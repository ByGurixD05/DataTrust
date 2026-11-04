package com.datatrust.domain.enums;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public enum ColumnType {
    STRING {
        @Override
        public boolean accepts(String value) {
            return true;
        }
    },
    INTEGER {
        @Override
        public boolean accepts(String value) {
            try {
                Long.parseLong(value.strip());
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
    },
    DECIMAL {
        @Override
        public boolean accepts(String value) {
            try {
                new BigDecimal(value.strip());
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
    },
    BOOLEAN {
        @Override
        public boolean accepts(String value) {
            String v = value.strip().toLowerCase(Locale.ROOT);
            return v.equals("true") || v.equals("false");
        }
    },
    // ISO-8601 (aaaa-mm-dd)
    DATE {
        @Override
        public boolean accepts(String value) {
            try {
                LocalDate.parse(value.strip());
                return true;
            } catch (DateTimeParseException ex) {
                return false;
            }
        }
    };

    /** Indica si el texto del CSV es un valor válido de este tipo. Los vacíos los decide la columna (required). */
    public abstract boolean accepts(String value);
}
