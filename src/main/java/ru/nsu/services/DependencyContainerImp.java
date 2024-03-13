package ru.nsu.services;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import ru.nsu.model.BeanDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

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
        beanDefinitions.put(name, beanDefinition);
    }

    public void registerSingletonBeanInstance(@NonNull BeanDefinition beanDefinition, Object beanInstance) {
        singletonInstances.put(beanDefinition.getClassName(), beanInstance);
    }

    public void registerThreadBeanInstance(@NonNull BeanDefinition beanDefinition, Supplier<?> beanSupplier) {
        threadInstances.put(beanDefinition.getClassName(), ThreadLocal.withInitial(beanSupplier));
    }

    public BeanDefinition getBeanDefinitionByName(String name) {
        return beanDefinitions.get(name);
    }

}

