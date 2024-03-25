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
 * Тут мы храним бины и достаём мы её тоже из этого класса.
 */
@Data
@NoArgsConstructor
@Slf4j
public class BeanContainer {

    private Map<String, BeanDefinition> beanDefinitions = new HashMap<>();

    private Map<String, Object> singletonInstances = new HashMap<>();

    private Map<String, ThreadLocal<Object>> threadInstances = new HashMap<>();

    private Map<String, Object> customBean = new HashMap<>();

    private DependencyScanningConfig dependencyScanningConfig;


    public BeanContainer(DependencyScanningConfig dependencyScanningConfig){
        this.dependencyScanningConfig = dependencyScanningConfig;
        this.beanDefinitions = dependencyScanningConfig.getNameToBeanDefitionMap();
    }

    @SuppressWarnings("all")
    public <T> T getThreadLocalBean(String name) {
        ThreadLocal<?> threadLocal = threadInstances.get(name);
        if (threadLocal != null) {
            return (T) threadLocal.get();
        }
        return null;
    }

    public boolean containsBean(String beanName) {
        return singletonInstances.containsKey(beanName) || threadInstances.containsKey(beanName);
    }

    public void registerSingletonBeanInstance(@NonNull BeanDefinition beanDefinition, Object beanInstance) {
        MDC.put("beanName", (beanDefinition.getName() != null ? beanDefinition.getName() : beanDefinition.getClassName()));
        log.info("Registering singleton bean instance for class");
        MDC.remove("beanName");
        singletonInstances.put((beanDefinition.getName() != null ? beanDefinition.getName() : beanDefinition.getClassName()), beanInstance);
    }

    public void registerThreadBeanInstance(@NonNull BeanDefinition beanDefinition, Supplier<?> beanSupplier) {
        MDC.put("beanName", (beanDefinition.getName() != null ? beanDefinition.getName() : beanDefinition.getClassName()));
        log.info("Registering thread-local bean instance");
        MDC.remove("beanName");
        threadInstances.put((beanDefinition.getName() != null ? beanDefinition.getName() : beanDefinition.getClassName()), ThreadLocal.withInitial(beanSupplier));
    }

    public void registerCustomBeanBeanInstance(@NonNull BeanDefinition beanDefinition, Object beanInstance) {
        MDC.put("beanName", (beanDefinition.getName() != null ? beanDefinition.getName() : beanDefinition.getClassName()));
        log.info("Registering singleton bean instance for class");
        MDC.remove("beanName");
        customBean.put((beanDefinition.getName() != null ? beanDefinition.getName() : beanDefinition.getClassName()), beanInstance);
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
                    MDC.put("beanName", ("all beans"));
                    log.error("Can't find directory with logs");
                    MDC.remove("beanName");
                }
            });
        }
        return logsForBean;
    }
}

