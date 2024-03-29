package ru.nsu.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.Serial;

/**
 * Ошибка, возникающая в ситуации, когда не была найдена необходимая зависимость
 * для внедрения.
 */
@Slf4j
public class NoDependencyException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Конструктор для выброса этой ошибки.
     *
     * @param name имя бина, который связан с ошибкой.
     */
    public NoDependencyException(String name, String description) {
        super("No dependency with name '" + name + "'. Description = " + description);
        MDC.put("beanName", name);
        log.error("No bean with this name found injections. " + description);
        MDC.remove("beanName");
    }
}
