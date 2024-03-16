package ru.nsu.services;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.nsu.exception.NoDependencyException;
import ru.nsu.exception.WrongJsonException;
import ru.nsu.model.BeanDefinition;
import ru.nsu.model.BeanDefinitionReader;
import ru.nsu.model.BeanDefinitionsWrapper;

import java.util.HashMap;
import java.util.Map;

@Data
@Slf4j
public class ScanningConfig {

    private final Map<String, BeanDefinition> singletonBeans = new HashMap<>();
    private final Map<String, BeanDefinition> prototypeBeans = new HashMap<>();
    private final Map<String, BeanDefinition> threadBeans = new HashMap<>();


    public void startBeanScanning(BeanDefinitionsWrapper beanDefinitions) {
        for (BeanDefinitionReader beanDefinition : beanDefinitions.getBeans()) {
            BeanDefinition definition = convertToBeanDefinition(beanDefinition);
            switch (beanDefinition.getScope().toLowerCase()) {
                case "singleton":
                    getSingletonBeans().put(beanDefinition.getClassName(), definition);
                    break;
                case "prototype":
                    getPrototypeBeans().put(beanDefinition.getClassName(), definition);
                    break;
                case "thread":
                    getThreadBeans().put(beanDefinition.getClassName(), definition);
                    break;
                default: {
                    MDC.put("beanName", beanDefinition.getClassName());
                    log.info("No such bean scope: " + definition.getScope());
                    MDC.remove("beanName");
                    throw new WrongJsonException(" no such bean scope: " + definition.getScope());
                }
            }
        }
    }


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


    private static BeanDefinition convertToBeanDefinition(BeanDefinitionReader reader) {
        return new BeanDefinition(reader.getClassName(), reader.getName(),
                reader.getScope(), reader.getInitParams(), reader.getConstructorParams());
    }
}
