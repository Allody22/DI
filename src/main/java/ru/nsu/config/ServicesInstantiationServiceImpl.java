package ru.nsu.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.nsu.model.BeanDefinition;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class ServicesInstantiationServiceImpl {

    private final DependencyContainerImp dependencyContainer;
    private final ScanningConfig scanningConfig;

    public ServicesInstantiationServiceImpl(DependencyContainerImp dependencyContainer, ScanningConfig scanningConfig) {
        this.dependencyContainer = dependencyContainer;
        this.scanningConfig = scanningConfig;
    }

    public void instantiateAndRegisterBeans() {
        // Перебираем все бины и регистрируем их определения в контейнере
        scanningConfig.getSingletonBeans().values().forEach(beanDefinition ->
                dependencyContainer.registerBeanDefinition(beanDefinition.getClassName(), beanDefinition));
        scanningConfig.getPrototypeBeans().values().forEach(beanDefinition ->
                dependencyContainer.registerBeanDefinition(beanDefinition.getClassName(), beanDefinition));
        scanningConfig.getThreadBeans().values().forEach(beanDefinition ->
                dependencyContainer.registerBeanDefinition(beanDefinition.getClassName(), beanDefinition));

        // Обработка singleton бинов
        instantiateAndRegisterScopeBeans(scanningConfig.getSingletonBeans(), "singleton");
        // Обработка prototype бинов
        instantiateAndRegisterScopeBeans(scanningConfig.getPrototypeBeans(), "prototype");
        // Обработка thread бинов
        instantiateAndRegisterScopeBeans(scanningConfig.getThreadBeans(), "thread");
    }

    private void instantiateAndRegisterScopeBeans(Map<String, BeanDefinition> beans, String scope) {
        beans.values().forEach(beanDefinition -> {
            if (!dependencyContainer.containsBean(beanDefinition.getName())) {
                Object beanInstance = createBeanInstance(beanDefinition);
                dependencyContainer.registerBeanInstance(beanDefinition.getClassName(), beanInstance);
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
            throw new RuntimeException("Не получилось инициализировать бин с именем " + beanDefinition.getClassName(), e);
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
            boolean matches = true;
            for (int i = 0; i < paramTypes.length; i++) {
                BeanDefinition paramDefinition = dependencyContainer.getBeanDefinitionByName((String) constructorParams.get(i));
                if (paramDefinition == null) {
                    log.info("Не нашли конструктор");
                    matches = false;
                    break;
                } else if (!paramTypes[i].isAssignableFrom(getClassForName(paramDefinition.getClassName()))){
                    log.info("Не подходящие типы для " + paramDefinition.getClassName() + " и для " + paramTypes[i]);
                    matches = false;
                    break;

                }
            }
            if (matches) {
                return constructor;
            }
        }
        throw new IllegalArgumentException("Не получилось использовать конструктор для " + beanClass.getName());
    }

    private Object[] resolveConstructorParameters(List<Object> constructorParams) {
        Object[] params = new Object[constructorParams.size()];
        for (int i = 0; i < constructorParams.size(); i++) {
            String beanName = (String) constructorParams.get(i);
            Object paramInstance = dependencyContainer.getBeanByName(beanName);
            if (paramInstance == null) {
                BeanDefinition depBeanDefinition = scanningConfig.findBeanDefinition(beanName);
                if (depBeanDefinition == null) {
                    throw new IllegalArgumentException("Dependency not found: " + beanName);
                }
                paramInstance = createBeanInstance(depBeanDefinition);
                // Вместо регистрации BeanDefinition как экземпляра, создаем и регистрируем реальный объект
                dependencyContainer.registerBeanInstance(beanName, paramInstance);
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
                String methodName = entry.getKey(); // Используйте ключ напрямую
                Object value = entry.getValue();
                Method setterMethod = findMethodByNameAndParameterType(instance.getClass(), methodName, value);
                setterMethod.invoke(instance, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to apply init param " + entry.getKey() + " to " + instance.getClass().getName(), e);
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
