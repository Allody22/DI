package ru.nsu.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.Serial;

/**
 * Ошибка, когда не получается вызвать срабатывание PostConstruct метод у какого-то бина.
 */
@Slf4j
public class PostConstructException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Конструктор для выбрасывания ошибки.
     *
     * @param beanName поле или имя бина с которым.
     * @param description описание ошибка.
     */
    public PostConstructException(String beanName, String description) {
        super("Error with PostConstruct method for '" + beanName + "'. Description: " + description);
        MDC.put("beanName", beanName);
        log.error(description);
        MDC.remove("beanName");
    }
}
