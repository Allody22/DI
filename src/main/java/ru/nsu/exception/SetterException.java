package ru.nsu.exception;

import java.io.Serial;

public class SetterException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SetterException(String firstValue, String secondValue) {
        super("Cant set the value of " + firstValue + " to: " + secondValue);
    }
}
