package ru.nsu.exception;

import java.io.Serial;

public class ConstructorClassMismatchException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConstructorClassMismatchException(String beanName, String requiredClass, String providedClass ) {
        super("Error with constructor of bean '" + beanName + "'. Required class: " + requiredClass + ", but provided is " + providedClass);
    }
}
