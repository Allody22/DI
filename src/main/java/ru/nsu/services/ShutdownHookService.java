package ru.nsu.services;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.nsu.exception.NoDependencyException;
import ru.nsu.exception.PreDestroyException;
import ru.nsu.model.BeanDefinition;

import javax.inject.Provider;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

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
     * Метод для вызова искусственного тестового уничтожения бинов
     * и проверки обработки PreDestroy аннотации. Этот метод вызывает приватный метод уничтожения,
     * чтобы не нарушить логику DI цикла жизни.
     */
    public void cleanupBeansForTest() {
        cleanupBeans();
    }


    /**
     * Метод, который вызывает срабатывание PreDestroy аннотации.
     * Сначала метод проходится по всем синглетон бинам в текущем DI контейнере,
     * затем внутри каждого из них проходится по полям и смотрит, является ли это поле prototype бином,
     * а если оно является и у него есть PreDestroy метод, то он вызывается и у этого бина.
     * Потом аналогичные действия происходят и для бинов типа thread.
     */
    private void cleanupBeans() {
        var beanDefinitions = beanContainer.getOrderedByDependenciesBeans();
        var singletonInstances = beanContainer.getSingletonInstances();
        Collections.reverse(beanDefinitions);
        for (var currentBeanName : beanDefinitions) {
            BeanDefinition beanDefinition = beanContainer.getBeanDefinitions().get(currentBeanName);
            Object beanInstance = null;
            if (beanDefinition == null) {
                throw new RuntimeException("Почему-то такого бина нет");
            }
            if (beanDefinition.getScope().equals("singleton")) {
                beanInstance = singletonInstances.get(currentBeanName);
            } else if (beanDefinition.getScope().equals("thread")) {
                beanInstance = beanContainer.getThreadLocalBean(currentBeanName);
            } else if (beanDefinition.getScope().equals("prototype")) {
                continue;
            }
            if (beanInstance == null) {
                throw new NoDependencyException(currentBeanName, "Error in shutdownHookService, can't found bean instance with this name.");
            }
            checkForPrototypeBeans(beanInstance);
            invokePreDestroy(beanInstance, beanDefinition);

        }
    }

    public void checkForPrototypeBeans(Object beanInstance) {
        for (Field field : beanInstance.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);

                Object potentialPrototypeDependency = field.get(beanInstance);

                if (potentialPrototypeDependency instanceof Provider) {
                    potentialPrototypeDependency = ((Provider<?>) potentialPrototypeDependency).get();
                }
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

