package tests;

import model.json.MyRepository;
import model.json.MyService;
import org.junit.jupiter.api.Test;
import ru.nsu.services.BeanContainer;
import ru.nsu.services.BeanInstanceService;
import ru.nsu.services.DependencyScanningConfig;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Проверка того, что наш DI правильно работает только с JSON конфигурации,
 * когда человек не хочет использоваться аннотации из javax.injection.
 */
public class JsonOnlyTest {

    @Test
    public void testSingletonBean() throws IOException, ClassNotFoundException {
        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForJsonOnlyConfig("beansOnlyJson.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        MyService firstInstance = beanInstanceService.getBean("myService");
        MyService secondInstance = beanInstanceService.getBean("myService");

        assertSame(firstInstance, secondInstance, "Это синглетон бины и они должны быть всегда одинаковые");
    }

    @Test
    public void testPrototypeBean() throws IOException, ClassNotFoundException {
        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForJsonOnlyConfig("beansOnlyJson.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        MyService serviceBean = beanInstanceService.getBean("myService");
        MyRepository firstMyRepository = beanInstanceService.getBean("myRepositoryImpl");
        MyRepository secondMyRepository = beanInstanceService.getBean("myRepositoryImpl");
        MyRepository thirdMyRepository = serviceBean.getMyRepository().get();

        assertAll("Прототайпы бинов не должны быть одинаковые",
                () -> assertNotSame(firstMyRepository, secondMyRepository),
                () -> assertNotSame(firstMyRepository, thirdMyRepository),
                () -> assertNotSame(secondMyRepository, thirdMyRepository)
        );
    }

    @Test
    public void testSetters() throws IOException, ClassNotFoundException {
        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForJsonOnlyConfig("beansOnlyJson.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        MyService myService = beanInstanceService.getBean("myService");
        MyRepository myRepository = beanInstanceService.getBean("myRepositoryImpl");

        assertEquals(myService.getSomeProperty(), "value1", "В значение someProperty в MyService из конфигурации установлена строка 'value1'");
        assertSame(myService.getAnotherProperty(), 5, "В значение anotherProperty в MyService из конфигурации установлено число 5");
        assertEquals(myRepository.getDataSource(), "myDataSource", "В значение myDataSource в MyRepository из конфигурации установлена строка 'myDataSource'");
    }
}
