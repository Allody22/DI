package ru.nsu.services;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.nsu.model.BeanDefinition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Контейнер для управления зависимостями, реализующий логику хранения и поиска
 * определений бинов и их экземпляров. Поддерживает бины различных областей видимости.
 */
@Data
@NoArgsConstructor
@Slf4j
public class DependencyContainerImp {
  /**
   * Словарь для хранения определений бинов.
   */
  private Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
  /**
   * Словарь для хранения экземпляров синглетон бинов.
   */
  private Map<String, Object> singletonInstances = new HashMap<>();
  /**
   * Словарь для хранения экземпляров потоково-специфичных бинов.
   */
  private Map<String, ThreadLocal<Object>> threadInstances = new HashMap<>();

  /**
   * Конфигурация сканирования для идентификации и регистрации бинов.
   */
  private ScanningConfig scanningConfig;

  /**
   * Конструктор для инициализации контейнера с заданной конфигурацией сканирования.
   *
   * @param scanningConfig Конфигурация сканирования.
   */
  public DependencyContainerImp(ScanningConfig scanningConfig) {
    this.scanningConfig = scanningConfig;
  }


  /**
   * Возвращает потоково-специфичный экземпляр бина по имени.
   *
   * @param name Имя бина.
   * @param <T>  Тип возвращаемого объекта.
   * @return Потоково-специфичный экземпляр бина или null, если не найден.
   */
  @SuppressWarnings("all")
  public <T> T getThreadLocalBean(String name) {
    ThreadLocal<?> threadLocal = threadInstances.get(name);
    if (threadLocal != null) {
      return (T) threadLocal.get();
    }
    return null;
  }

  /**
   * Проверяет, содержит ли контейнер бин с указанным именем.
   *
   * @param beanName Имя бина.
   * @return true, если бин существует в контейнере; иначе false.
   */
  public boolean containsBean(String beanName) {
    return singletonInstances.containsKey(beanName) || threadInstances.containsKey(beanName);
  }

  /**
   * Регистрирует определение модели (представлние в json) бина в контейнере.
   *
   * @param name           Имя бина.
   * @param beanDefinition Определение бина.
   */
  public void registerBeanDefinition(String name, BeanDefinition beanDefinition) {
        MDC.put("beanName", beanDefinition.getClassName());
        log.info("Registering bean definition");
        MDC.remove("beanName");
        beanDefinitions.put(name, beanDefinition);
    }

  /**
   * Регистрирует экземпляр синглетон бина в словаре синглетонов в контейнере.
   *
   * @param beanDefinition Определение бина.
   * @param beanInstance   Экземпляр бина.
   */
  public void registerSingletonBeanInstance(@NonNull BeanDefinition beanDefinition, Object beanInstance) {
        MDC.put("beanName", beanDefinition.getClassName());
        log.info("Registering singleton bean instance for class");
        MDC.remove("beanName");
        singletonInstances.put(beanDefinition.getClassName(), beanInstance);
    }

  /**
   * Регистрирует потоково-специфичный бин и его фабрику для создания экземпляров.
   *
   * @param beanDefinition Определение бина.
   * @param beanSupplier   Функция-поставщик экземпляров бина.
   */
  public void registerThreadBeanInstance(@NonNull BeanDefinition beanDefinition, Supplier<?> beanSupplier) {
        MDC.put("beanName", beanDefinition.getClassName());
        log.info("Registering thread-local bean instance");
        MDC.remove("beanName");
        threadInstances.put(beanDefinition.getClassName(), ThreadLocal.withInitial(beanSupplier));
    }

  /**
   * Возвращает определение бина по его имени.
   *
   * @param name Имя бина.
   * @return Определение бина или null, если бин не найден.
   */
  public BeanDefinition getBeanDefinitionByName(String name) {
    return beanDefinitions.get(name);
  }

    public List<String> getLogsForBeanInASpecialFile(String beanClassName, String logFilePath) throws IOException {
        List<String> logsForBean = new ArrayList<>();
        try (Stream<String> stream = Files.newBufferedReader(Paths.get(logFilePath), StandardCharsets.UTF_8).lines()) {
            stream.forEach(line -> {
                // Теперь фильтрация идёт по части сообщения, содержащей имя класса
                if (line.contains(beanClassName)) {
                    logsForBean.add(line);
                }
            });
        }
        return logsForBean;
    }


    public List<String> getLogsForBean(String beanClassName) throws IOException {
        String logDirPath = "logs";
        List<String> logsForBean = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(logDirPath))) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try (Stream<String> stream = Files.newBufferedReader(file, StandardCharsets.UTF_8).lines()) {
                    stream.forEach(line -> {
                        if (line.contains(beanClassName)) {
                            logsForBean.add(line);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        return logsForBean;
    }

}

