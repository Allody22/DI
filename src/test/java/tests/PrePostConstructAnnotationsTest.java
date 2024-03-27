package tests;

import model.pre_post.MyRepository;
import model.pre_post.MyService;
import model.pre_post.MySmsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ru.nsu.services.BeanContainer;
import ru.nsu.services.BeanInstanceService;
import ru.nsu.services.DependencyScanningConfig;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты на простых файлах участием JSON конфигурации и javax.injection.
 * Проверяем что PostConstruct и PreDestroy аннотации действительно правильно срабатывают.
 */
public class PrePostConstructAnnotationsTest {

    @Test
    public void testPostConstruct() throws IOException {
        System.setProperty("environment", "test");

        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForAnnotatedClasses("model.pre_post", "beans_pre_post.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        MyService myService = beanInstanceService.getBean("myServiceImplementation");
        assertEquals(myService.getCheckConstruct(),"Service @PostConstruct",
                "Значение сервиса только инициализировалось через @PostConstruct");
        assertEquals(myService.getMyRepository().getCheckConstruct(),"myRepo @PostConstruct",
                "Значение репозитория только инициализировалось через PostConstruct");
        MyRepository myRepository = beanInstanceService.getBean("myRepositoryImpl");

        assertEquals(myService.getMyRepository().getCheckConstruct(),myRepository.getCheckConstruct(),
                "Значение репозитория только инициализировалось через PostConstruct");

        beanContainer.testCleanup();

        assertEquals(myService.getCheckConstruct(),"Service @PreDestroy",
                "Сервис имитировал уничтожение через @PreDestroy и значение строки должно быть соответствующее");
        assertEquals(myService.getMyRepository().getCheckConstruct(),"myRepo @PreDestroy",
                "Репозиторий имитировал уничтожение через @PreDestroy  и значение строки должно быть myRepo @PreDestroy");
    }

    @Test
    public void testPostConstructWith2Values() throws IOException {
        System.setProperty("environment", "test");

        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForAnnotatedClasses("model.pre_post", "beans_pre_post.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        MySmsService myService = beanInstanceService.getBean("mySmsServiceImplementation");
        MyRepository myRepository = myService.getMyRepository().get();
        assertEquals(myService.getCheckConstruct(),"Service @PostConstruct",
                "Значение сервиса только инициализировалось через @PostConstruct");
        assertEquals(myService.getAnotherProperty(),128,
                "Значение сервиса только инициализировалось через @PostConstruct и должно быть равно 128");
        assertEquals(myRepository.getCheckConstruct(),"myRepo @PostConstruct",
                "Значение репозитория только инициализировалось через PostConstruct");

        beanContainer.testCleanup();

        assertEquals(myService.getCheckConstruct(),"Service @PreDestroy",
                "Сервис имитировал уничтожение через @PreDestroy и значение строки должно быть соответствующее");
        assertEquals(myService.getAnotherProperty(),300,
                "Сервис имитировал уничтожение через @PreDestroy и должно быть равно 300");
    }

    @AfterEach
    public void cleanup() {
        System.clearProperty("environment");
    }
}
