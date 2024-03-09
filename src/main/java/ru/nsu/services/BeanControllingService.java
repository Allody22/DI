package ru.nsu.services;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.nsu.exception.*;
import ru.nsu.model.BeanDefinition;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class BeanControllingService {

    private final DependencyContainerImp dependencyContainer;

    public BeanControllingService(DependencyContainerImp dependencyContainer) {
        this.dependencyContainer = dependencyContainer;
    }

    @SuppressWarnings("all")
    public <T> T getBeanByName(String name) {
        BeanDefinition definition = dependencyContainer.getBeanDefinitions().get(name);
        if (definition == null) {
            throw new WrongJsonException(" no bean : " + name);
        }
        return switch (definition.getScope()) {
            case "singleton" -> (T) dependencyContainer.getSingletonInstances().get(name);
            case "prototype" -> (T) createBeanInstance(definition);
            case "thread" -> dependencyContainer.getThreadLocalBean(name);
            default ->
                    throw new WrongJsonException(" no such bean scope: " + definition.getScope());
        };
    }

    public void instantiateAndRegisterBeans() {
        var singletonBeans = dependencyContainer.getScanningConfig().getSingletonBeans();
        var prototypeBeans = dependencyContainer.getScanningConfig().getPrototypeBeans();
        var threadBeans = dependencyContainer.getScanningConfig().getThreadBeans();

        singletonBeans.values().forEach(beanDefinition ->
                dependencyContainer.registerBeanDefinition(beanDefinition.getClassName(), beanDefinition));
        prototypeBeans.values().forEach(beanDefinition ->
                dependencyContainer.registerBeanDefinition(beanDefinition.getClassName(), beanDefinition));
        threadBeans.values().forEach(beanDefinition ->
                dependencyContainer.registerBeanDefinition(beanDefinition.getClassName(), beanDefinition));

        // Обработка singleton бинов
        instantiateAndRegisterScopeBeans(singletonBeans, "singleton");
        // Обработка thread бинов
        instantiateAndRegisterScopeBeans(threadBeans, "thread");
    }

    private void instantiateAndRegisterScopeBeans(Map<String, BeanDefinition> beans, String scope) {
        beans.values().forEach(beanDefinition -> {
            if (!dependencyContainer.containsBean(beanDefinition.getName())) {
                Object beanInstance = createBeanInstance(beanDefinition);
                if (scope.equals("thread")){
                    dependencyContainer.registerThreadBeanInstance(beanDefinition,  ()->createBeanInstance(beanDefinition));
                } else if (scope.equals("singleton")) {
                    dependencyContainer.registerSingletonBeanInstance(beanDefinition, beanInstance);
                }
            }
        });
    }

    public Object createBeanInstance(BeanDefinition beanDefinition) {
        try {
            Class<?> beanClass = Class.forName(beanDefinition.getClassName());
            Constructor<?> constructor = findSuitableConstructor(beanClass, beanDefinition.getConstructorParams());
            Object[] params = null;
            if (beanDefinition.getConstructorParams() != null && !beanDefinition.getConstructorParams().isEmpty()) {
                params = resolveConstructorParameters(beanDefinition.getConstructorParams());
            } else {
                params = new Object[0]; // Пустой массив для конструктора без параметров
            }
            Object instance = constructor.newInstance(params);
            applyInitParams(instance, beanDefinition.getInitParams());
            return instance;
        } catch (Exception e) {
            throw new ConstructorException(beanDefinition.getClassName());
        }
    }

    private Constructor<?> findSuitableConstructor(Class<?> beanClass, List<Object> constructorParams) {
        if (constructorParams == null || constructorParams.isEmpty()) {
            try {
                return beanClass.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("No default constructor found for " + beanClass.getName(), e);
            }
        }
        Constructor<?>[] constructors = beanClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length != constructorParams.size()) {
                continue;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                BeanDefinition paramDefinition = dependencyContainer.getBeanDefinitionByName((String) constructorParams.get(i));
                if (paramDefinition == null) {
                    throw new EmptyJsonException(beanClass.getName(),"bean name");
                } else if (!paramTypes[i].isAssignableFrom(getClassForName(paramDefinition.getClassName()))){
                    throw new ConstructorClassMismatchException(beanClass.getName(),paramTypes[i].toString(), paramDefinition.getClassName());
                }
            }
                return constructor;
        }
        throw new ConstructorException(beanClass.getName());
    }

    private Object[] resolveConstructorParameters(List<Object> constructorParams) {
        Object[] params = new Object[constructorParams.size()];
        for (int i = 0; i < constructorParams.size(); i++) {
            String beanName = (String) constructorParams.get(i);
            Object paramInstance = getBeanByName(beanName);
            if (paramInstance == null) {
                BeanDefinition beanDefinition = dependencyContainer.getScanningConfig().findBeanDefinition(beanName);
                if (beanDefinition == null) {
                    throw new NoDependencyException(beanName);
                }
                paramInstance = createBeanInstance(beanDefinition);
                if (beanDefinition.getScope().equals("thread")){
                    dependencyContainer.registerThreadBeanInstance(beanDefinition, ()->createBeanInstance(beanDefinition));
                } else if (beanDefinition.getScope().equals("singleton")) {
                    dependencyContainer.registerSingletonBeanInstance(beanDefinition, paramInstance);
                }
            }
            params[i] = paramInstance;
        }
        return params;
    }



    private Class<?> getClassForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
    }

    private void applyInitParams(Object instance, Map<String, Object> initParams) {
        if (initParams == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : initParams.entrySet()) {
            try {
                String methodName = entry.getKey();
                Object value = entry.getValue();
                Method setterMethod = findMethodByNameAndParameterType(instance.getClass(), methodName, value);
                setterMethod.invoke(instance, value);
            } catch (Exception e) {
                throw new SetterException(entry.getKey(), instance.getClass().getName());
            }
        }
    }

    private Method findMethodByNameAndParameterType(Class<?> clazz, String methodName, Object value) throws NoSuchMethodException {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
                return method;
            }
        }
        throw new NoSuchMethodException(clazz.getName() + "." + methodName + "(...)");
    }
}
