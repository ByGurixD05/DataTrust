package com.datatrust.application.validation;

/** El contenido subido no se puede leer en su formato (por ejemplo, un CSV con comillas sin cerrar). */
public class MalformedContentException extends RuntimeException {

    public MalformedContentException(String message, Throwable cause) {
        super(message, cause);
    }
}
