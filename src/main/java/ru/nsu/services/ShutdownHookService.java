package ru.nsu.services;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.nsu.exception.PreDestroyException;
import ru.nsu.model.BeanDefinition;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Сервис, отвечающий за вызов PreDestroy методов, если он имеется, у всех бинов.
 */
@Slf4j
public class ShutdownHookService {
    private final BeanContainer beanContainer;

    /**
     * Конструктор данного класса.
     *
     * @param beanContainer хранилище всех бинов, которые необходимо, чтобы получать все бины,
     *                      а потом вызывать у всех бинов их PreDestroy методы.
     */
    public ShutdownHookService(BeanContainer beanContainer) {
        this.beanContainer = beanContainer;
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanupBeans));
    }

    /**
     * Метод, который вызывает срабатывание PreDestroy аннотации.
     * Сначала метод проходится по всем синглетон бинам в текущем DI контейнере,
     * затем внутри каждого из них проходится по полям и смотрит, является ли это поле prototype бином,
     * а если оно является и у него есть PreDestroy метод, то он вызывается и у этого бина.
     * Потом аналогичные действия происходят и для бинов типа thread.
     */
    private void cleanupBeans() {
        var singletonInstances = beanContainer.getSingletonInstances();
        singletonInstances.forEach((name, singletonInstance) -> {
            BeanDefinition singletonDefinition = beanContainer.getBeanDefinitions().get(name);
            if (singletonDefinition != null) {
                for (Field field : singletonInstance.getClass().getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object potentialPrototypeDependency = field.get(singletonInstance);
                        if (potentialPrototypeDependency != null) {
                            BeanDefinition prototypeBeanDefinition = beanContainer.findPrototypeBeanDefinition(potentialPrototypeDependency.getClass().getName());
                            if (prototypeBeanDefinition != null && prototypeBeanDefinition.getPreDestroyMethod() != null) {
                                invokePreDestroy(potentialPrototypeDependency, prototypeBeanDefinition);
                            }
                        }
                    } catch (IllegalAccessException e) {
                        throw new PreDestroyException(field.getName(), "Exception with PreDestroy method " +
                                "of the prototype field of the singleton bean");
                    }
                }
                invokePreDestroy(singletonInstance, singletonDefinition);
            }
        });

        var threadBeans = beanContainer.getThreadInstances();
        threadBeans.forEach((name, threadLocalInstance) -> {
            BeanDefinition threadDefinition = beanContainer.getBeanDefinitions().get(name);
            if (threadDefinition != null) {
                for (Field field : threadLocalInstance.getClass().getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object potentialPrototypeDependency = field.get(threadLocalInstance);
                        if (potentialPrototypeDependency != null) {
                            BeanDefinition prototypeBeanDefinition = beanContainer.findPrototypeBeanDefinition(potentialPrototypeDependency.getClass().getName());
                            if (prototypeBeanDefinition != null && prototypeBeanDefinition.getPreDestroyMethod() != null) {
                                invokePreDestroy(potentialPrototypeDependency, prototypeBeanDefinition);
                            }
                        }
                    } catch (IllegalAccessException e) {
                        throw new PreDestroyException(field.getName(), "Exception with PreDestroy method " +
                                "of the prototype field of the singleton bean");
                    }
                }
                invokePreDestroy(threadLocalInstance, threadDefinition);
            }
        });
    }

    /**
     * Вывоз PreDestroy метода у инстанса бина.
     *
     * @param beanInstance   сам инстанс бина.
     * @param beanDefinition модель бина для получения PreDestroy метода из рефлексии.
     */
    private void invokePreDestroy(Object beanInstance, BeanDefinition beanDefinition) {
        if (beanDefinition.getPreDestroyMethod() != null) {
            try {
                Method preDestroyMethod = beanDefinition.getPreDestroyMethod();
                preDestroyMethod.setAccessible(true);
                preDestroyMethod.invoke(beanInstance);
                MDC.put("beanName", beanDefinition.getName());
                log.info("Successfully invoked @PreDestroy method with name '{}'.", preDestroyMethod.getName());
                MDC.remove("beanName");
            } catch (Exception e) {
                throw new PreDestroyException(beanDefinition.getName(), "Exception with invoking of PreDestroy method");
            }
        }
    }
}

