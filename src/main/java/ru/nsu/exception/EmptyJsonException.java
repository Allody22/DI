package ru.nsu.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.Serial;

/**
 * Ошибка, возникающая при отсутствии информации об обязательном поле в JSON конфигурации.
 */
@Slf4j
public class EmptyJsonException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Выводим ошибку, связанную с отсутствием информации об обязательном поле в JSON конфигурации и
     * записываем информацию о ней в файл для логов.
     * @param beanName название бина для лучшего ориентира.
     * @param jsonErrorField подробное описание ошибки.
     */
    public EmptyJsonException(String beanName, String jsonErrorField) {
        super("Error with bean '" + beanName + "'. Json reading error with field: " + jsonErrorField);
        MDC.put("beanName", beanName);
        log.error("Json reading error with field:" + jsonErrorField);
        MDC.remove("beanName");
    }
}
