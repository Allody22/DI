package ru.nsu.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.MDC;
import ru.nsu.exception.ClazzExceptionException;
import ru.nsu.exception.ConstructorException;
import ru.nsu.exception.EmptyJsonException;
import ru.nsu.exception.WrongJsonException;
import ru.nsu.model.BeanDefinition;
import ru.nsu.model.BeanDefinitionReader;
import ru.nsu.model.BeanDefinitionsWrapper;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Data
@Slf4j
public class DependencyScanningConfig {

    private Map<String, BeanDefinition> nameToBeanDefitionMap = new HashMap<>();

    private Map<String, BeanDefinition> singletonScopes = new HashMap<>();

    private Map<String, BeanDefinition> threadScopes = new HashMap<>();

    private Map<String, BeanDefinition> unknownScopes = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<BeanDefinitionReader> beansFromJson;

    public void scanForAnnotatedClasses(String scanningDirectory, String jsonConfig) throws IOException {
        Reflections reflections = new Reflections(scanningDirectory,
                new SubTypesScanner(false),
                new TypeAnnotationsScanner());


        this.beansFromJson = readBeanDefinitions(jsonConfig).getBeans();

        Set<Class<?>> allClasses = reflections.getSubTypesOf(Object.class);

        for (Class<?> clazz : allClasses) {
            if (!clazz.isInterface() && isAvailableForInjection(clazz)) {
                Named namedAnnotation = clazz.getAnnotation(Named.class);
                String namedAnnotationValue;
                if (namedAnnotation != null) {
                    namedAnnotationValue = namedAnnotation.value();
                } else {
                    throw new ClazzExceptionException(clazz.getCanonicalName());
                }

                BeanDefinitionReader beanDefinitionReader = findBeanInJson(namedAnnotationValue);
                if (beanDefinitionReader == null) {
                    MDC.put("beanName", clazz.getName());
                    log.error("No such bean in JSON config");
                    MDC.remove("beanName");
                    throw new WrongJsonException(". No configuration for bean with name: " + namedAnnotationValue);
                }
                //Проверяем все филды, где есть инъекция. Потенциально с провайдеров
                List<Field> injectedFields = new ArrayList<>();
                List<Field> injectedProviderFields = new ArrayList<>();
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Inject.class)) {
                        if (Provider.class.isAssignableFrom(field.getType())) {
                            injectedProviderFields.add(field);
                        } else {
                            injectedFields.add(field);
                        }
                    }
                }

                Constructor<?> selectedConstructor = null;
                boolean isConstructorFound = false;

                // Поиск конструктора с аннотацией @Inject
                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                    if (constructor.isAnnotationPresent(Inject.class)) {
                        selectedConstructor = constructor;
                        isConstructorFound = true;
                        break; // Найден конструктор для инъекции
                    }
                }

                // Если конструктор с @Inject не найден, используем конструктор по умолчанию, если он есть
                if (!isConstructorFound) {
                    try {
                        selectedConstructor = clazz.getDeclaredConstructor(); // Конструктор без параметров
                    } catch (NoSuchMethodException e) {
                        throw new ConstructorException(clazz.getCanonicalName(), "No constructor at all");
                    }
                }

                if ((!injectedFields.isEmpty() && isConstructorFound) || (!injectedProviderFields.isEmpty() && isConstructorFound)) {
                    throw new ConstructorException(clazz.getCanonicalName(), "Only one type of injection is available: fields or constructors");
                }

                // Проверяем наличие аннотации @Named или условия для добавления класса
                BeanDefinition beanDefinition = new BeanDefinition(
                        clazz.getCanonicalName(),
                        namedAnnotationValue,
                        beanDefinitionReader.getScope(),
                        injectedFields.isEmpty() ? null : injectedFields, // Если список пуст, устанавливаем в null
                        injectedProviderFields.isEmpty() ? null : injectedProviderFields, // Если список пуст, устанавливаем в null
                        selectedConstructor, //Конструктор, который нашли
                        beanDefinitionReader.getInitParams()
                );
                this.nameToBeanDefitionMap.put(namedAnnotationValue, beanDefinition);
                switch (beanDefinitionReader.getScope()) {
                    case "prototype" -> {
                    }
                    case "singleton" -> singletonScopes.put(namedAnnotationValue, beanDefinition);
                    case "thread" -> threadScopes.put(namedAnnotationValue, beanDefinition);
                    //TODO кастомный скоуп?
                    default -> unknownScopes.put(namedAnnotationValue, beanDefinition);
                }
            }
        }
    }

    public BeanDefinitionsWrapper readBeanDefinitions(String jsonConfigPath) throws IOException {
        String fullPath = "beans/" + jsonConfigPath;
        InputStream jsonInput = this.getClass().getClassLoader().getResourceAsStream(fullPath);
        return objectMapper.readValue(jsonInput, BeanDefinitionsWrapper.class);
    }


    public void scanForJsonOnlyConfig(String jsonConfigPath) throws ClassNotFoundException, NoSuchMethodException, IOException {
        this.beansFromJson = readBeanDefinitions(jsonConfigPath).getBeans();
        for (BeanDefinitionReader currentBean : beansFromJson) {
            if (currentBean.getName() == null){
                throw new EmptyJsonException("unknown", "No name field for this json");
            }
            if (currentBean.getScope() == null){
                throw new EmptyJsonException(currentBean.getName(), "No scope field for this bean in json");
            }
            if (currentBean.getClassName() == null){
                throw new EmptyJsonException(currentBean.getName(), "No className field for this bean in json");
            }
            String scope = currentBean.getScope();
            String beanName = currentBean.getName();
            BeanDefinition beanDefinition = new BeanDefinition();
            beanDefinition.setScope(scope);
            beanDefinition.setName(beanName);
            beanDefinition.setClassName(currentBean.getClassName());
            beanDefinition.setInitParams(currentBean.getInitParams());

            if (currentBean.getConstructorParams() != null && !currentBean.getConstructorParams().isEmpty()) {
                // Преобразование списка Object в список String
                List<String> paramTypeNames = currentBean.getConstructorParams().stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
                Constructor<?> constructor = findAndSetConstructor(currentBean.getClassName(), paramTypeNames);
                beanDefinition.setConstructor(constructor);
            }
            switch (scope) {
                case "singleton" -> singletonScopes.put(beanName, beanDefinition);
                case "prototype" -> {
                }
                case "thread" -> threadScopes.put(beanName, beanDefinition);
                default -> {
                    unknownScopes.put(beanName, beanDefinition);
                }
            }
            nameToBeanDefitionMap.put(beanName, beanDefinition);
        }
    }

    private Constructor<?> findAndSetConstructor(String className, List<String> paramTypeNames) throws ClassNotFoundException, NoSuchMethodException {
        Class<?> clazz = Class.forName(className);
        List<Class<?>> paramClasses = new ArrayList<>();

        for (String paramName : paramTypeNames) {
            if (paramName.startsWith("Provider<") && paramName.endsWith(">")) {
                String actualParamTypeName = paramName.substring("Provider<".length(), paramName.length() - 1);
                Class<?> actualParamType = Class.forName(actualParamTypeName);
                // Используем рефлексию для получения типа Provider с конкретным параметром
                paramClasses.add(javax.inject.Provider.class);
            } else {
                paramClasses.add(Class.forName(paramName));
            }
        }

        Class<?>[] paramTypes = paramClasses.toArray(new Class[0]);
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (Arrays.equals(constructor.getParameterTypes(), paramTypes)) {
                return constructor;
            }
        }

        throw new ConstructorException(className, "Can't make suitable construct for json config.");
    }


    private boolean isAvailableForInjection(Class<?> clazz) {
        // Проверка наличия @Named на уровне класса
        if (clazz.isAnnotationPresent(Named.class)) {
            return true;
        }

        // Проверка полей, конструкторов и методов на наличие аннотаций @Inject или @Named
        // Класс подходит, если есть хотя бы одна аннотация @Inject или @Named
        return (Stream.of(clazz.getDeclaredFields(), clazz.getDeclaredConstructors(), clazz.getDeclaredMethods())
                .flatMap(Arrays::stream)
                .anyMatch(member -> member.isAnnotationPresent(Inject.class) || member.isAnnotationPresent(Named.class)));
    }


    private BeanDefinitionReader findBeanInJson(String namedAnnotationValue) {
        for (BeanDefinitionReader currentBeanJson : beansFromJson) {
            if (namedAnnotationValue.equals(currentBeanJson.getName())) {
                return currentBeanJson;
            }
        }
        return null;
    }
}
