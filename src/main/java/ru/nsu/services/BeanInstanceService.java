package ru.nsu.services;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.nsu.exception.*;
import ru.nsu.model.BeanDefinition;

import javax.inject.Named;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.Map;

/**
 * Этот класс отвечает за создание инстансов бинов и их дальнейшее сохранение в контейнер бинов.
 */
@Data
@Slf4j
public class BeanInstanceService {

    private final BeanContainer beanContainer;

    /**
     * Публичный конструктор сервиса создания бинов.
     *
     * @param beanContainer контейнер бинов, связанный с этим сервис.
     *                      Информация из него будет получаться для использования в создании бинов,
     *                      а дальнейшие инстансы бинов будут сохраняться в этом контейнере.
     */
    public BeanInstanceService(BeanContainer beanContainer) {
        this.beanContainer = beanContainer;
    }

    /**
     * Получение бина по его имени. Бин может быть любого типа и scope.
     * Если этот бин еще не создан, то будет попытка создать его, а если это не увенчается успехом,
     * то выпадет соответствующая ошибка.
     *
     * @param name имя бина.
     * @param <T>  ожидаемый тип возвращаемого бина. Предостережение: тип не проверяется при выполнении,
     *             поэтому неправильное использование может привести к {@code ClassCastException}.
     * @return инстанс бина.
     */
    @SuppressWarnings("all")
    public <T> T getBean(String name) {
        MDC.put("beanName", name);
        log.info("Attempting to get bean by class name");
        MDC.remove("beanName");
        BeanDefinition definition = null;
        var allBeans = beanContainer.getBeanDefinitions();
        definition = allBeans.get(name);
        if (definition == null) {
            for (var singleBean : allBeans.values()) {
                if (singleBean.getClassName().equals(name) || singleBean.getName().equals(name)) {
                    definition = singleBean;
                    break;
                }
            }
        }
        T result = switch (definition.getScope()) {
            case "singleton" -> (T) beanContainer.getSingletonInstances().get(name);
            case "prototype" -> (T) createBeanInstance(definition);
            case "thread" -> beanContainer.getThreadLocalBean(name);
            default -> {
                MDC.put("beanName", name);
                log.warn("No such bean scope: " + definition.getScope());
                MDC.remove("beanName");
                throw new WrongJsonException(definition.getName(), ".No such bean scope: " + definition.getScope());
            }
        };
        if (definition.getScope().equals("prototype")) {
            invokePostConstruct(result, definition);
        }
        MDC.put("beanName", name);
        log.info("Successfully retrieved bean");
        MDC.remove("beanName");
        return result;
    }

    /**
     * Метод, который запускает DI цикл.
     * Получается список моделей отсортированных бинов из контейнера,
     * а затем эти бины начинают от наибольших (те у кого больше всего зависимостей)
     * до наименьших (те у кого вообще нет зависимостей) создаваться.
     */
    public void instantiateAndRegisterBeans() {
        var beanDefinitions = beanContainer.getBeanDefinitions();
        var orderedBeanNames = beanContainer.getOrderedByDependenciesBeans();

        Collections.reverse(orderedBeanNames);
        // Проходим по упорядоченному списку и создаем/регистрируем бины
        orderedBeanNames.forEach(beanName -> {
            BeanDefinition beanDefinition = beanDefinitions.get(beanName);
            instantiateAndRegisterBean(beanDefinition);
        });
    }

    /**
     * Создаём и сохраняем инстанс определённого бина, основываясь на его модели.
     * Этот метод запускаем с самого начала, вместе с запуском самого процесса DI, поэтому
     * тут не создаются инстансы бинов типа prototype, их сущность создаётся по мере необходимости,
     * то есть когда они запрашиваются.
     *
     * @param beanDefinition модель бина.
     */
    private void instantiateAndRegisterBean(BeanDefinition beanDefinition) {
            String beanName = (beanDefinition.getName() != null) ? beanDefinition.getName() : beanDefinition.getClassName();
            String beanScope = beanDefinition.getScope();
            if (beanScope.equals("prototype")){
                return;
            }
            if (!beanContainer.containsBean(beanName)) {
                Object beanInstance = createBeanInstance(beanDefinition);
                invokePostConstruct(beanInstance, beanDefinition);
                if (beanScope.equals("thread")) {
                    beanContainer.registerThreadBeanInstance(beanDefinition, () -> createBeanInstance(beanDefinition));
                } else if (beanScope.equals("singleton")) {
                    beanContainer.registerSingletonBeanInstance(beanDefinition, beanInstance);
                }
            }
        }

    /**
     * Вызов PostConstruct метода, привязанного к бину.
     * В зависимости от цикла жизни бина этот метод вызывается в разное время.
     * Например, у prototype бина этот метод вызывается только когда его прямо запрашивает,
     * как и у бинов обёрнутых в Provider, а у бинов типа Singleton и thread этот метод вызывается после создания инстанса.
     *
     * @param beanInstance   инстанс бина
     * @param beanDefinition модель, описывающая бин
     */
    private void invokePostConstruct(Object beanInstance, BeanDefinition beanDefinition) {
        Method postConstructMethod = beanDefinition.getPostConstructMethod();
        if (postConstructMethod != null) {
            try {
                postConstructMethod.setAccessible(true);
                postConstructMethod.invoke(beanInstance);
                MDC.put("beanName", beanDefinition.getName());
                log.info("Successfully started PostConstruct method");
                MDC.remove("beanName");
            } catch (Exception e) {
                throw new PostConstructException(beanDefinition.getName(), "Failed to invoke PostConstruct method");
            }
        }
    }

    /**
     * Функция для создания самого инстанса бина, а затем его сохранения в контейнере.
     * Происходит проверка на наличие всей необходимой информации в модели бина, иначе выбрасывается ошибка.
     * Создаются и применяются конструкторы, запускается процесс установки обычных полей в классе
     * или их inject, если это необходимо для бина.
     *
     * @param beanDefinition модель со всей информации о бине.
     * @return объект, представляющий бин.
     */
    public Object createBeanInstance(BeanDefinition beanDefinition) {
        String beanName = (beanDefinition.getName() != null) ? beanDefinition.getName() : beanDefinition.getClassName();

        MDC.put("beanName", beanName);
        log.info("Trying to create bean instance");
        MDC.remove("beanName");
        try {
            Class<?> beanClass = Class.forName(beanDefinition.getClassName());
            Constructor<?> constructor = beanDefinition.getConstructor();
            if (constructor == null || constructor.getParameters().length == 0) {
                // Если конструктор не был задан, ищем конструктор по умолчанию
                constructor = beanClass.getDeclaredConstructor();
            }
            Object[] constructorParams = resolveConstructorParameters(constructor);
            Object instance = constructor.newInstance(constructorParams);

            var injectedFields = beanDefinition.getInjectedFields();
            var injectedProviderFields = beanDefinition.getInjectedProviderFields();

            //Внедряем все поля-зависимости, если они есть
            if (injectedFields != null && !injectedFields.isEmpty()) {
                for (Field field : injectedFields) {
                    field.setAccessible(true);
                    Object fieldInstance = injectField(field);
                    field.set(instance, fieldInstance);
                }
            }


            if (injectedProviderFields != null && !injectedProviderFields.isEmpty()) {
                for (Field field : injectedProviderFields) {
                    field.setAccessible(true);
                    Object fieldInstance = injectField(field);
                    Provider<?> providerField = () -> fieldInstance;
                    field.set(instance, providerField);
                }
            }

            // Дополнительная инициализация, если требуется
            applyInitParams(instance, beanDefinition.getInitParams());
            return instance;
        } catch (Exception e) {
            throw new ConstructorException(beanName, "Failed to create instance. " + e.getMessage());
        }
    }

    /**
     * Код для внедрения полей-зависимостей, то есть полей,
     * помеченных аннотацией Named и Inject
     *
     * @param field сущность поля.
     * @return созданный инстанс зависимости для дальнейшего внедрения.
     */
    private Object injectField(Field field) {
        field.setAccessible(true);
        Named namedAnnotation = field.getAnnotation(Named.class);
        String actualName = (namedAnnotation != null ? namedAnnotation.value() : field.getName());
        Object fieldInstance = getBean(actualName);
        BeanDefinition newFieldBeanDefinition;
        if (fieldInstance == null) {
            newFieldBeanDefinition = beanContainer.getBeanDefinitions().get(actualName);
            fieldInstance = createAndRegisterBeanDependency(newFieldBeanDefinition);
        }
        return fieldInstance;
    }

    /**
     * Метод, чтобы создать бин-зависимость для другого бина,
     * то есть когда мы пытаемся внедрить бин, который еще не был создан
     * или имеет тип prototype, то мы вызываем этот метод.
     *
     * @param beanDefinition модель бина.
     * @return инстанс бина.
     */
    private Object createAndRegisterBeanDependency(BeanDefinition beanDefinition) {
        Object beanInstance = createBeanInstance(beanDefinition);
        invokePostConstruct(beanInstance, beanDefinition);
        switch (beanDefinition.getScope()) {
            case "thread" ->
                    beanContainer.registerThreadBeanInstance(beanDefinition, () -> createBeanInstance(beanDefinition));
            case "singleton" -> beanContainer.registerSingletonBeanInstance(beanDefinition, beanInstance);
            case "prototype" -> {
            }
        }
        if (beanInstance == null) {
            throw new NoDependencyException(beanDefinition.getName(), "Error in creating of dependency, can't create instance for this name.");
        }

        return beanInstance;
    }

    /**
     * Метод для поиска параметров переданного конструктора бина.
     * Идёт дополнительная проверка на то, является ли параметр в конструкторе Provider,
     * ищутся параметры в конструкторе по аннотации Named, а если необходимо в процессе
     * создать другой бин, который еще не был зарегистрирован, то он создаётся и регистрируется.
     *
     * @param constructor класс конструктора из библиотеки рефлексии.
     * @return созданный набор параметров конструктора.
     */
    private Object[] resolveConstructorParameters(Constructor<?> constructor) {
        Class<?>[] paramTypes = constructor.getParameterTypes();
        Object[] params = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            if (Provider.class.isAssignableFrom(paramTypes[i])) {
                // Получаем актуальный тип для Provider
                Class<?> actualType = (Class<?>) ((ParameterizedType) constructor.getGenericParameterTypes()[i]).getActualTypeArguments()[0];
                Provider<?> provider = () -> getBean(actualType.getName());
                params[i] = provider;
            } else {
                // Пытаемся получить имя из аннотации @Named для параметра, если оно есть
                Annotation[] annotations = constructor.getParameterAnnotations()[i];
                String namedValue = null;
                for (Annotation annotation : annotations) {
                    if (annotation instanceof Named) {
                        namedValue = ((Named) annotation).value();
                        break;
                    }
                }
                Object paramsResult;
                String actualName;

                // Определяем actualName в зависимости от того, задано ли namedValue.
                actualName = (namedValue != null) ? namedValue : paramTypes[i].getName();

                // Пытаемся получить bean с использованием actualName.
                paramsResult = getBean(actualName);

                // Если bean не найден, создаем его инстанс.
                if (paramsResult == null) {
                    BeanDefinition beanDefinition = beanContainer.getBeanDefinitions().get(actualName);
                    paramsResult = createAndRegisterBeanDependency(beanDefinition);
                }

                params[i] = paramsResult;
            }
        }
        return params;
    }


    /**
     * Установка параметров (полей) в инстанс бина.
     *
     * @param instance   уже готовый, созданный инстанс бина
     * @param initParams параметры бина, полученные из конфигурации.
     */
    private void applyInitParams(Object instance, Map<String, Object> initParams) {
        if (initParams == null || initParams.isEmpty()) {
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

    /**
     * Поиск метода для установки параметров определённого типа в инстанс бина.
     *
     * @param clazz      класс, параметры готова мы ищем.
     * @param methodName название метода.
     * @param value      значение параметра.
     * @return сущность метод из рефлексии.
     * @throws NoSuchMethodException ошибка, в случае когда метод не был найден.
     */
    private Method findMethodByNameAndParameterType(Class<?> clazz, String methodName, Object value) throws NoSuchMethodException {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
                return method;
            }
        }
        MDC.put("beanName", clazz.getName());
        log.error("No such method: " + methodName);
        MDC.remove("beanName");
        throw new NoSuchMethodException(clazz.getName() + "." + methodName + "(...)");
    }
}
