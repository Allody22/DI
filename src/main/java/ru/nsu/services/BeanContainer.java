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
 * В этом классе мы храним все модели и инстансы бинов.
 */
@Data
@NoArgsConstructor
@Slf4j
public class BeanContainer {

    private Map<String, BeanDefinition> beanDefinitions = new HashMap<>();

    private Map<String, Object> singletonInstances = new HashMap<>();

    private List<String> orderedByDependenciesBeans = new ArrayList<>();

    private Map<String, ThreadLocal<Object>> threadInstances = new HashMap<>();

    private Map<String, Object> customBean = new HashMap<>();

    private DependencyScanningConfig dependencyScanningConfig;

    /**
     * Специальный тестовый метода для вызова PreDestroy аннотаций у бинов.
     */
    public void testCleanup() {
        if (!"test".equals(System.getProperty("environment"))) {
            throw new IllegalStateException("This method is intended for testing purposes only.");
        }
        new ShutdownHookService(this).cleanupBeansForTest();
    }


    /**
     * Конструктор контейнера бинов, записывающий сюда всю просканированную информацию.
     * Тут также устанавливается ShutDownHookService, который при остановке программы,
     * вызовет все PreDestroy методы.
     * На этой стадии также идёт проверка бинов на циклические зависимости
     * с помощью DependencyResolver, который представляет все связи бинов как граф,
     * а с помощью специального готово метода потом проверяет его на цикл.
     *
     * @param dependencyScanningConfig конфиг сканирования.
     */
    public BeanContainer(DependencyScanningConfig dependencyScanningConfig) {
        this.dependencyScanningConfig = dependencyScanningConfig;
        this.beanDefinitions = dependencyScanningConfig.getNameToBeanDefinitionMap();
        DependencyResolver resolver = new DependencyResolver(beanDefinitions);

        this.orderedByDependenciesBeans = resolver.resolveDependencies();


        new ShutdownHookService(this);
    }

    /**
     * Ищем, существует ли такая модель prototype бина среди известных просканированных бинов.
     *
     * @param beanName название бина или название класса бина.
     * @return найденная модель бина или null иначе.
     */
    public BeanDefinition findPrototypeBeanDefinition(String beanName) {
        for (var currentBean : beanDefinitions.values()) {
            if (currentBean.getName().equals(beanName) || currentBean.getClassName().equals(beanName)) {
                if (currentBean.getScope().equals("prototype")) {
                    return currentBean;
                }
            }
        }
        return null;
    }

    /**
     * Получает экземпляр бина, связанного с текущим потоком, по его имени.
     * Этот метод предназначен для использования с бинами, имеющими область видимости "thread",
     * что означает, что каждый поток имеет свою собственную уникальную инстанцию бина.
     * Если бин с указанным именем не найден или не является бином типа "thread",
     * метод возвращает {@code null}.
     *
     * @param name имя бина, инстанс которого необходимо получить. Не должно быть {@code null}.
     * @param <T>  ожидаемый тип возвращаемого бина. Предостережение: тип не проверяется при выполнении,
     *             поэтому неправильное использование может привести к {@code ClassCastException}.
     * @return инстанс бина типа "thread" для текущего потока или {@code null}, если такой бин не найден
     * или имя {@code name} не соответствует бину типа "thread".
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
     * Проверяем, есть ли уже созданный экземпляр синглетон или потокового бина,
     * потому что у нас нет необходимости создать их еще раз.
     *
     * @param beanName имя бина.
     * @return true если такой бин есть, иначе false.
     */
    public boolean containsBean(String beanName) {
        return singletonInstances.containsKey(beanName) || threadInstances.containsKey(beanName);
    }

    /**
     * Регистрируем инстанс синглетон бина и сохраняем его в контейнер.
     *
     * @param beanDefinition модель бина, чтобы получить его известное имя.
     * @param beanInstance   инстанс бина.
     */
    public void registerSingletonBeanInstance(@NonNull BeanDefinition beanDefinition, Object beanInstance) {
        MDC.put("beanName", (beanDefinition.getName() != null ? beanDefinition.getName() : beanDefinition.getClassName()));
        log.info("Registering singleton bean instance for class");
        MDC.remove("beanName");
        singletonInstances.put((beanDefinition.getName() != null ? beanDefinition.getName() : beanDefinition.getClassName()), beanInstance);
    }

    /**
     * Регистрируем инстанс потокового бина и сохраняем его в контейнер.
     *
     * @param beanDefinition модель бина, чтобы получить его известное имя.
     * @param beanSupplier   инстанс потокового бина, обёрнутый в Supplier, чтобы он сохранился в определённый поток.
     */
    public void registerThreadBeanInstance(@NonNull BeanDefinition beanDefinition, Supplier<?> beanSupplier) {
        MDC.put("beanName", (beanDefinition.getName() != null ? beanDefinition.getName() : beanDefinition.getClassName()));
        log.info("Registering thread-local bean instance");
        MDC.remove("beanName");
        threadInstances.put((beanDefinition.getName() != null ? beanDefinition.getName() : beanDefinition.getClassName()), ThreadLocal.withInitial(beanSupplier));
    }

    /**
     * Функция для получения логов бина по его имени из специального файла.
     *
     * @param beanClassName имя бина, для которого мы будем искать логи.
     * @return набор json логов.
     * @throws IOException ошибка, на случай если файл с логами не был найден.
     */
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
                    MDC.put("beanName", ("all beans"));
                    log.error("Can't find directory with logs");
                    MDC.remove("beanName");
                }
            });
        }
        return logsForBean;
    }
}

