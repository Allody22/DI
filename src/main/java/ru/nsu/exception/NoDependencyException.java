package ru.nsu.exception;

import java.io.Serial;

public class NoDependencyException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NoDependencyException(String name) {
        super("No dependency with name '" + name + "'");
    }
}
