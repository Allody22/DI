package org.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.annotations.PostConstruct;
import org.example.annotations.PreDestroy;
import org.example.model.BeanDefinitionReader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class BeanCreator {

    private ObjectMapper objectMapper = new ObjectMapper();

    public Object createBeanInstance(BeanDefinitionReader beanDefinition) throws Exception {
        Class<?> clazz = Class.forName(beanDefinition.getClassName());
        Constructor<?> constructor = findConstructor(clazz, beanDefinition.getConstructorParams());
        Object[] constructorArgs = getConstructorArguments(beanDefinition.getConstructorParams(), constructor);
        return constructor.newInstance(constructorArgs);
    }

    private Constructor<?> findConstructor(Class<?> clazz, List<Object> constructorParams) throws NoSuchMethodException, ClassNotFoundException {
        outerLoop:
        for (Constructor<?> constructor : clazz.getConstructors()) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == constructorParams.size()) {
                for (int i = 0; i < paramTypes.length; i++) {
                    Class<?> paramClass = Class.forName(constructorParams.get(i).toString());
                    if (!paramTypes[i].isAssignableFrom(paramClass)) {
                        continue outerLoop;
                    }
                }
                return constructor;
            }
        }
        throw new NoSuchMethodException("No suitable constructor found for " + clazz);
    }


    private Object[] getConstructorArguments(List<Object> constructorParams, Constructor<?> constructor) {
        Object[] args = new Object[constructorParams.size()];
        Class<?>[] paramTypes = constructor.getParameterTypes();

        for (int i = 0; i < constructorParams.size(); i++) {
            Object param = constructorParams.get(i);
            Class<?> paramType = paramTypes[i];
            Object arg = objectMapper.convertValue(param, paramType);
            args[i] = arg;
        }

        return args;
    }

    public void injectDependencies(Object bean, BeanDefinitionReader beanDefinition) throws Exception {
        for (Map.Entry<String, Object> entry : beanDefinition.getInitParams().entrySet()) {
            Method setter = findSetterMethod(bean, entry.getKey());
            if (setter != null) {
                Object value = convertValueToRequiredType(entry.getValue(), setter.getParameterTypes()[0]);
                setter.invoke(bean, value);
            }
        }
    }

    private Method findSetterMethod(Object bean, String propertyName) {
        String methodName = "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        for (Method method : bean.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterTypes().length == 1) {
                return method;
            }
        }
        return null;
    }

    private Object convertValueToRequiredType(Object value, Class<?> requiredType) {
        return objectMapper.convertValue(value, requiredType);
    }

    public void initializeBean(Object bean) throws Exception {
        for (Method method : bean.getClass().getMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                method.invoke(bean);
            }
        }
    }

    public void destroyBean(Object bean) throws Exception {
        for (Method method : bean.getClass().getMethods()) {
            if (method.isAnnotationPresent(PreDestroy.class)) {
                method.invoke(bean);
            }
        }
    }


}
