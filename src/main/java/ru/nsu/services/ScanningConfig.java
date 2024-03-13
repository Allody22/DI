package ru.nsu.services;

import lombok.Data;
import ru.nsu.exception.NoDependencyException;
import ru.nsu.exception.WrongJsonException;
import ru.nsu.model.BeanDefinition;
import ru.nsu.model.BeanDefinitionReader;
import ru.nsu.model.BeanDefinitionsWrapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс {@code ScanningConfig} управляет конфигурацией сканирования бинов, разделяя их на синглетоны,
 * прототипы и бины на уровне потока. Каждый тип бина хранится в отдельной мапе для быстрого доступа.
 */
@Data
public class ScanningConfig {

  private final Map<String, BeanDefinition> singletonBeans = new HashMap<>();
  private final Map<String, BeanDefinition> prototypeBeans = new HashMap<>();
  private final Map<String, BeanDefinition> threadBeans = new HashMap<>();


  /**
   * Сканирует определения бинов из предоставленного объекта {@code BeanDefinitionsWrapper} и классифицирует их
   * в соответствии с их областью видимости (singleton, prototype, thread).
   *
   * @param beanDefinitions объект {@code BeanDefinitionsWrapper}, содержащий определения бинов для сканирования
   * @throws WrongJsonException если область видимости бина не распознана
   */
  public void startBeanScanning(BeanDefinitionsWrapper beanDefinitions) {
    for (BeanDefinitionReader beanDefinition : beanDefinitions.getBeans()) {
      BeanDefinition definition = convertToBeanDefinition(beanDefinition);
      switch (beanDefinition.getScope().toLowerCase()) {
        case "singleton" -> getSingletonBeans().put(beanDefinition.getClassName(), definition);
        case "prototype" -> getPrototypeBeans().put(beanDefinition.getClassName(), definition);
        case "thread" -> getThreadBeans().put(beanDefinition.getClassName(), definition);
        default -> throw new WrongJsonException(" no such bean scope: " + definition.getScope());
      }
    }
  }


  /**
   * Ищет определение бина по его имени. Поиск ведется сначала среди синглетонов, затем среди бинов на уровне потока,
   * и, наконец, среди прототипов.
   *
   * @param beanName имя бина для поиска
   * @return определение бина {@code BeanDefinition}
   * @throws NoDependencyException если определение бина не найдено
   */
  public BeanDefinition findBeanDefinition(String beanName) {
    // Сначала ищем в синглетонах
    BeanDefinition beanDefinition = singletonBeans.get(beanName);
    if (beanDefinition != null) {
      return beanDefinition;
    }
    beanDefinition = threadBeans.get(beanName);
    if (beanDefinition != null) {
      return beanDefinition;
    }
    // Затем в прототипах
    beanDefinition = prototypeBeans.get(beanName);
    if (beanDefinition != null) {
      return beanDefinition;
    } else {
      throw new NoDependencyException(beanName);
    }
  }

  /**
   * Преобразует объект {@code BeanDefinitionReader} в {@code BeanDefinition}.
   * Этот метод служит вспомогательной функцией для преобразования прочитанных данных в формат определения бина.
   *
   * @param reader объект {@code BeanDefinitionReader}, содержащий сырые данные определения бина
   * @return преобразованный объект {@code BeanDefinition}
   */
  private static BeanDefinition convertToBeanDefinition(BeanDefinitionReader reader) {
    return new BeanDefinition(reader.getClassName(), reader.getName(),
        reader.getScope(), reader.getInitParams(), reader.getConstructorParams());
  }
}
