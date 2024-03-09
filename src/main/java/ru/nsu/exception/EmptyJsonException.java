package ru.nsu.exception;

import java.io.Serial;

public class EmptyJsonException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EmptyJsonException(String beanName, String jsonErrorField) {
        super("Error with bean '" + beanName + "'. Json reading error with field : " + jsonErrorField);
    }
}
