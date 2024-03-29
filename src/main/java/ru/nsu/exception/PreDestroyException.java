package ru.nsu.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.Serial;

/**
 * Ошибка, когда не получается вызвать PreDestroy метод у какого-то класса.
 */
@Slf4j
public class PreDestroyException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Конструктор для выбрасывания ошибки.
     *
     * @param beanName поле или имя бина с которым.
     * @param description описание ошибка.
     */
    public PreDestroyException(String beanName, String description) {
        super("Error with PreDestroy method for '" + beanName + "'. Description: " + description);
        MDC.put("beanName", beanName);
        log.error(description);
        MDC.remove("beanName");
    }
}
