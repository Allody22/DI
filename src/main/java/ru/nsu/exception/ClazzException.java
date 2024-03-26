package ru.nsu.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.Serial;

/**
 * Ошибка, когда Named аннотация в классе, используемом для DI не была найдена.
 */
@Slf4j
public class ClazzException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Конструктор, выбрасываемой ошибки.
     *
     * @param className имя класса.
     */
    public ClazzException(String className) {
        super("Error with class '" + className + "'. No @Named found" );
        MDC.put("beanName", className);
        log.error("No @Named for this clas");
        MDC.remove("beanName");
    }
}
