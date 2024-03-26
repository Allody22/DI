package ru.nsu.exception;

import java.io.Serial;

/**
 * Ошибка, возникающая при невозможности установить значения параметров из конфигурации.
 */
public class SetterException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Конструктор для выбрасывания этой ошибки.
     *
     * @param firstValue первое значение параметра.
     * @param secondValue второе значение параметра.
     */
    public SetterException(String firstValue, String secondValue) {
        super("Cant set the value of " + firstValue + " to: " + secondValue);
    }
}
