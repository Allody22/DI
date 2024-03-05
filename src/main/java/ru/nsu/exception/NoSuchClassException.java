package ru.nsu.exception;

import java.io.Serial;

public class NoSuchClassException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NoSuchClassException(String beanName, String problem ) {
        super("Error with bean '" + beanName + "'. No such class: " + problem);
    }
}
