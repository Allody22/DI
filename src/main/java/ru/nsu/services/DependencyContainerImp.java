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

@Data
@NoArgsConstructor
@Slf4j
public class DependencyContainerImp {
    private Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
    private Map<String, Object> singletonInstances = new HashMap<>();
    private Map<String, ThreadLocal<Object>> threadInstances = new HashMap<>();

    private ScanningConfig scanningConfig;

    public DependencyContainerImp(ScanningConfig scanningConfig) {
        this.scanningConfig = scanningConfig;
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

    public void registerBeanDefinition(String name, BeanDefinition beanDefinition) {
        MDC.put("beanName", beanDefinition.getClassName());
        log.info("Registering bean definition");
        MDC.remove("beanName");
        beanDefinitions.put(name, beanDefinition);
    }

    public void registerSingletonBeanInstance(@NonNull BeanDefinition beanDefinition, Object beanInstance) {
        MDC.put("beanName", beanDefinition.getClassName());
        log.info("Registering singleton bean instance for class");
        MDC.remove("beanName");
        singletonInstances.put(beanDefinition.getClassName(), beanInstance);
    }

    public void registerThreadBeanInstance(@NonNull BeanDefinition beanDefinition, Supplier<?> beanSupplier) {
        MDC.put("beanName", beanDefinition.getClassName());
        log.info("Registering thread-local bean instance");
        MDC.remove("beanName");
        threadInstances.put(beanDefinition.getClassName(), ThreadLocal.withInitial(beanSupplier));
    }

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

