package ru.nsu.config;

import lombok.Data;
import ru.nsu.model.BeanDefinition;
import ru.nsu.model.BeanDefinitionReader;
import ru.nsu.model.BeanDefinitionsWrapper;

import java.util.HashMap;
import java.util.Map;

@Data
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
                default:
                    throw new RuntimeException("Какая-то странная область видимости бина");
            }
        }
    }


    public BeanDefinition findBeanDefinition(String beanName) {
        // Сначала ищем в синглетонах
        BeanDefinition beanDefinition = singletonBeans.get(beanName);
        if (beanDefinition != null) {
            return beanDefinition;
        }
        // Затем в прототипах
        return prototypeBeans.get(beanName);
    }


    private static BeanDefinition convertToBeanDefinition(BeanDefinitionReader reader) {
        return new BeanDefinition(reader.getClassName(), reader.getName(),
                reader.getScope(), reader.getInitParams(), reader.getConstructorParams());
    }
}
