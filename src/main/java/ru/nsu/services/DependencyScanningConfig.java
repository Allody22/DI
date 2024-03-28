package ru.nsu.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import ru.nsu.exception.ClazzException;
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
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Класс, используемый для сканирования и изучения директории с бинами
 * и для чтения JSON конфигурации с бинами.
 */
@Data
@Slf4j
public class DependencyScanningConfig {

    private Map<String, BeanDefinition> nameToBeanDefinitionMap = new HashMap<>();

    private Map<String, BeanDefinition> singletonScopes = new HashMap<>();

    private Map<String, BeanDefinition> threadScopes = new HashMap<>();

    private Map<String, BeanDefinition> unknownScopes = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<BeanDefinitionReader> beansFromJson;

    /**
     * Мы сканируем классы в определённом пакете, с помощью рефлексию смотрим все аннотации в этих классах.
     * В первую очередь смотрим на аннотации Named, Inject и на интерфейс Provider.
     * А затем мы сопоставляем эту информацию с конфигурации из json бинов.
     * Если получаются какие-то несоответствия или информация не полная, то будет выброшена специальная ошибка.
     *
     * @param scanningDirectory директория, в которой мы будем изучать классы с помощью reflection.
     * @param jsonConfig        файл из папки resources/beans в котором находится json конфигурация бинов.
     * @throws IOException ошибка, возникающая, если директории, переданные в функции не были найдены.
     */
    public void scanForAnnotatedClasses(String scanningDirectory, String jsonConfig) throws IOException {
        Reflections reflections = new Reflections(scanningDirectory,
                new SubTypesScanner(false),
                new TypeAnnotationsScanner());


        this.beansFromJson = readBeanDefinitions(jsonConfig).getBeans();

        Set<Class<?>> allClasses = reflections.getSubTypesOf(Object.class);

        for (Class<?> clazz : allClasses) {
            if (!clazz.isInterface() && isAvailableForInjection(clazz)) {
                BeanDefinition beanDefinition = new BeanDefinition();
                String namedAnnotationValue = Optional.ofNullable(clazz.getAnnotation(Named.class))
                        .map(Named::value)
                        .orElseThrow(() -> new ClazzException(clazz.getCanonicalName()));

                BeanDefinitionReader beanDefinitionReader = Optional.ofNullable(findBeanInJson(namedAnnotationValue))
                        .orElseThrow(() -> new WrongJsonException(namedAnnotationValue, ". No configuration for bean with name."));

                List<Field> injectedFields = new ArrayList<>();
                List<Field> injectedProviderFields = new ArrayList<>();
                analyzeClassFields(clazz, injectedFields, injectedProviderFields);

                Constructor<?> selectedConstructor = null;
                boolean isConstructorFound = false;

                // Проверяем, есть ли конструктор с аннотацией inject
                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                    if (constructor.isAnnotationPresent(Inject.class)) {
                        selectedConstructor = constructor;
                        isConstructorFound = true;
                        break;
                    }
                }

                // Если конструктор с @Inject не найден, используем конструктор по умолчанию, если он есть.
                if (!isConstructorFound) {
                    try {
                        selectedConstructor = clazz.getDeclaredConstructor();
                    } catch (NoSuchMethodException e) {
                        throw new ConstructorException(clazz.getCanonicalName(), "No constructor at all");
                    }
                }

                // Если были найдены поля класса, участвующие в инъекции и при этом есть конструктор с инъекцией, то выбрасывается ошибка
                if ((!injectedFields.isEmpty() && isConstructorFound) || (!injectedProviderFields.isEmpty() && isConstructorFound)) {
                    throw new ConstructorException(clazz.getCanonicalName(), "Only one type of injection is available: fields or constructors");
                }
                beanDefinition.setClassName(clazz.getCanonicalName());
                beanDefinition.setName(namedAnnotationValue);
                beanDefinition.setScope(beanDefinitionReader.getScope());
                beanDefinition.setInjectedFields(injectedFields.isEmpty() ? null : injectedFields);
                beanDefinition.setInjectedProviderFields(injectedProviderFields.isEmpty() ? null : injectedProviderFields);
                beanDefinition.setConstructor(selectedConstructor);
                beanDefinition.setInitParams(beanDefinitionReader.getInitParams());
                findConstructMethods(clazz, beanDefinition);

                this.nameToBeanDefinitionMap.put(namedAnnotationValue, beanDefinition);
                switch (beanDefinitionReader.getScope()) {
                    case "prototype" -> {
                    }
                    case "singleton" -> singletonScopes.put(namedAnnotationValue, beanDefinition);
                    case "thread" -> threadScopes.put(namedAnnotationValue, beanDefinition);
                    default -> throw new WrongJsonException(namedAnnotationValue, "Unknown bean scope " + beanDefinition.getScope());
                }
            }
        }
    }

    /**
     * Поиск метода для создания конструктора. Его логика вынесена для упрощения.
     *
     * @param clazz интересующий нас класс.
     * @param beanDefinition модель, исследуемого бина.
     */
    private void findConstructMethods(Class<?> clazz, BeanDefinition beanDefinition) {
        Method postConstructMethod = null;
        Method preDestroyMethod = null;

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class) && method.getParameterCount() == 0) {
                if (postConstructMethod == null) {
                    postConstructMethod = method;
                } else {
                    throw new IllegalStateException("@PostConstruct annotation found on multiple methods in " + clazz.getName());
                }
            }

            if (method.isAnnotationPresent(PreDestroy.class) && method.getParameterCount() == 0) {
                if (preDestroyMethod == null) {
                    preDestroyMethod = method;
                } else {
                    throw new IllegalStateException("@PreDestroy annotation found on multiple methods in " + clazz.getName());
                }
            }
        }

        beanDefinition.setPostConstructMethod(postConstructMethod);
        beanDefinition.setPreDestroyMethod(preDestroyMethod);
    }

    /**
     * Проходимся по полям класса и смотрит, какие поля необходимо будет внедрять,
     * а какие из них еще и являются провайдером.
     *
     * @param clazz                  интересующий нас класс.
     * @param injectedFields         поля, участвующие во внедрении.
     * @param injectedProviderFields поля провайдера, участвующие во внедрении.
     */
    private void analyzeClassFields(Class<?> clazz, List<Field> injectedFields, List<Field> injectedProviderFields) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                if (Provider.class.isAssignableFrom(field.getType())) {
                    injectedProviderFields.add(field);
                } else {
                    injectedFields.add(field);
                }
            }
        }
    }


    /**
     * Считываем json конфигурацию бинов и получаем их модель.
     *
     * @param jsonConfigPath путь, в котором находится конфигурация бинов.
     * @return список из моделей бинов.
     * @throws IOException ошибка, на случай если файла конфигурации не существует.
     */
    private BeanDefinitionsWrapper readBeanDefinitions(String jsonConfigPath) throws IOException {
        String fullPath = "beans/" + jsonConfigPath;
        InputStream jsonInput = this.getClass().getClassLoader().getResourceAsStream(fullPath);
        return objectMapper.readValue(jsonInput, BeanDefinitionsWrapper.class);
    }

    /**
     * Создаём сущности бинов, основываясь только на json конфигурации файлов,
     * то есть мы не ищем с помощью рефлексии аннотации в определённых пакетах,
     * а полностью опираемся на конфигурацию.
     *
     * @param jsonConfigPath расположение файла конфигурации.
     * @throws ClassNotFoundException ошибка, если указанный в конфигурации файл не существует.
     * @throws IOException            ошибка, если файла с конфигурацией не существует.
     */
    public void scanForJsonOnlyConfig(String jsonConfigPath) throws ClassNotFoundException, IOException {
        this.beansFromJson = readBeanDefinitions(jsonConfigPath).getBeans();
        for (BeanDefinitionReader currentBean : beansFromJson) {
            if (currentBean.getName() == null) {
                throw new EmptyJsonException("unknown", "No name field for this json");
            }
            if (currentBean.getScope() == null) {
                throw new EmptyJsonException(currentBean.getName(), "No scope field for this bean in json");
            }
            if (currentBean.getClassName() == null) {
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
                default -> throw new WrongJsonException(beanName, "Unknown bean scope.");
            }
            nameToBeanDefinitionMap.put(beanName, beanDefinition);
        }
    }

    /**
     * Создаём конструктор для класса, полученного из json конфигурации,
     * чтобы мы могли превратить бин из конфигурации в полностью BeanDefinition модель.
     * Если в джейсоне в конструкторе указан Provider, то мы передаем в конструктор его.
     *
     * @param className      имя класса, который мы будем сканировать.
     * @param paramTypeNames список параметров конструктора.
     * @return сущность конструктора класса.
     * @throws ClassNotFoundException ошибка, когда по переданному className не был найден класс в проекте.
     */
    private Constructor<?> findAndSetConstructor(String className, List<String> paramTypeNames) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        List<Class<?>> paramClasses = new ArrayList<>();

        for (String paramName : paramTypeNames) {
            if (paramName.startsWith("Provider<") && paramName.endsWith(">")) {
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

    /**
     * Проверяем, можем ли мы использовать найденный класс для DI.
     * Если у класса вообще нет Named аннотаций и Inject аннотаций, то он
     * не включает в цикл жизни контейнера.
     *
     * @param clazz интересующий нас класс.
     * @return true, если класс является бином.
     */
    private boolean isAvailableForInjection(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Named.class)) {
            return true;
        }

        return (Stream.of(clazz.getDeclaredFields(), clazz.getDeclaredConstructors(), clazz.getDeclaredMethods())
                .flatMap(Arrays::stream)
                .anyMatch(member -> member.isAnnotationPresent(Inject.class) || member.isAnnotationPresent(Named.class)));
    }


    /**
     * Ищем в JSON конфигурации класс, по его названию, чтобы получить полную информацию о классе.
     *
     * @param namedAnnotationValue название бина из аннотации Named или из названия класса.
     * @return найденная сущность бина.
     */
    private BeanDefinitionReader findBeanInJson(String namedAnnotationValue) {
        for (BeanDefinitionReader currentBeanJson : beansFromJson) {
            if (namedAnnotationValue.equals(currentBeanJson.getName())) {
                return currentBeanJson;
            }
        }
        return null;
    }
}
