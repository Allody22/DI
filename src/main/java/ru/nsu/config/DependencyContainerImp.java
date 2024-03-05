package ru.nsu.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.nsu.model.BeanDefinition;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
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


    public <T> T getBeanByName(String name) {
        BeanDefinition definition = beanDefinitions.get(name);
        if (definition == null) {
            throw new IllegalArgumentException("В джейсоне не было найдено бина с именем: " + name);
        }
        switch (definition.getScope()) {
            case "singleton":
                return (T) singletonInstances.get(name);
            case "prototype":
                // Здесь создаем новый экземпляр каждый раз
                return (T) createBeanInstance(definition);
            case "thread":
                return (T) threadInstances.get(name).get();
            default:
                throw new IllegalArgumentException("Неподдерживаемый тип жизненного цикла: " + definition.getScope()
                        + " для бина с именем " + name);
        }
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
            throw new RuntimeException("Failed to create bean instance for " + beanDefinition.getClassName(), e);
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
                BeanDefinition paramDefinition = getBeanDefinitionByName((String) constructorParams.get(i));
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
            Object paramInstance = getBeanByName(beanName);
            if (paramInstance == null) {
                BeanDefinition depBeanDefinition = scanningConfig.findBeanDefinition(beanName);
                if (depBeanDefinition == null) {
                    throw new IllegalArgumentException("Dependency not found: " + beanName);
                }
                paramInstance = createBeanInstance(depBeanDefinition);
                // Вместо регистрации BeanDefinition как экземпляра, создаем и регистрируем реальный объект
                registerBeanInstance(beanName, paramInstance);
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

    public boolean containsBean(String beanName) {
        return singletonInstances.containsKey(beanName) || threadInstances.containsKey(beanName);
    }

    public void registerBeanDefinition(String name, BeanDefinition beanDefinition) {
        beanDefinitions.put(name, beanDefinition);
    }


    public void registerBeanInstance(String name, Object beanInstance) {
        BeanDefinition definition = beanDefinitions.get(name);
        if (definition != null) {
            switch (definition.getScope()) {
                case "singleton":
                    singletonInstances.put(name, beanInstance);
                    break;
                case "thread":
                    threadInstances.put(name, ThreadLocal.withInitial(() -> beanInstance));
                    break;
            }
        } else {
            throw new IllegalArgumentException("No bean definition found for " + name);
        }
    }


    public BeanDefinition getBeanDefinitionByName(String name) {
        return beanDefinitions.get(name);
    }
    public void registerThreadLocalBean(String name, Supplier<?> beanSupplier) {
        ThreadLocal<Object> threadLocal = ThreadLocal.withInitial(beanSupplier);
        threadInstances.put(name, threadLocal);
    }

    public <T> T getThreadLocalBean(String name) {
        ThreadLocal<?> threadLocal = threadInstances.get(name);
        if (threadLocal != null) {
            return (T) threadLocal.get();
        }
        return null;
    }

}

