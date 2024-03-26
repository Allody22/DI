package ru.nsu.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.Serial;

/**
 * Ошибка, возникающая при невозможности создать конструктор для бина.
 */
@Slf4j
public class ConstructorException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Выводим ошибку, связанную с конструктором с подробным описанием и
     * записываем информацию о ней в файл для логов.
     * @param beanName название бина для лучшего ориентира.
     * @param description подробное описание ошибки.
     */
    public ConstructorException(String beanName, String description) {
        super("Error with bean '" + beanName + "'." + description);
        MDC.put("beanName", beanName);
        log.error(description);
        MDC.remove("beanName");
    }
}
