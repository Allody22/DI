package ru.nsu.exception;

import java.io.Serial;

public class ConstructorException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConstructorException(String beanName) {
        super("Error with bean '" + beanName + "'. Something went wrong with constructor" );
    }
}
