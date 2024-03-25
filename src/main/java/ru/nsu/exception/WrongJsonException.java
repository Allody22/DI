package ru.nsu.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.Serial;

/**
 * Ошибка, связанная с информацией о бине в JSON конфигурации.
 */
@Slf4j
public class WrongJsonException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Выводим ошибку, связанную с информацией о бине в JSON конфигурации и
     * записываем информацию о ней в файл для логов.
     * @param beanName название бина для лучшего ориентира.
     * @param description подробное описание ошибки.
     */
    public WrongJsonException(String beanName, String description) {
        super("Error with json for bean " + beanName + description);
        MDC.put("beanName", beanName);
        log.error(description);
        MDC.remove("beanName");
    }
}
