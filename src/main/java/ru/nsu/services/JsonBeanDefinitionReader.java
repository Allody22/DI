package ru.nsu.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.nsu.model.BeanDefinitionsWrapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * Класс предназначен для чтения определений бинов из JSON-файла.
 * Использует библиотеку Jackson для десериализации JSON в объекты Java.
 **/
@Data
@NoArgsConstructor
public class JsonBeanDefinitionReader {

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Читает и десериализует определения бинов из JSON-файла по указанному пути.
   *
   * @param jsonConfigPath Путь к JSON-файлу конфигурации внутри ресурсов проекта.
   * @return BeanDefinitionsWrapper объект, содержащий определения бинов.
   * @throws IOException В случае ошибок ввода/вывода, например, если файл не найден.
   */
  public BeanDefinitionsWrapper readBeanDefinitions(String jsonConfigPath) throws IOException {
    InputStream jsonInput = this.getClass().getClassLoader().getResourceAsStream(jsonConfigPath);
    return objectMapper.readValue(jsonInput, BeanDefinitionsWrapper.class);
  }
}
