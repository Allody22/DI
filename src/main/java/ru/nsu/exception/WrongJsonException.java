package ru.nsu.exception;

import java.io.Serial;

public class WrongJsonException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public WrongJsonException(String beanName) {
        super("Error with json" + beanName);
    }
}
