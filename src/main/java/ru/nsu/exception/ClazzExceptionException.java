package ru.nsu.exception;

import java.io.Serial;

public class ClazzExceptionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ClazzExceptionException(String className) {
        super("Error with class '" + className + "'. No @Named found" );
    }
}
