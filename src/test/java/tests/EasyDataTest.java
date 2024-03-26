package tests;

import model.easy.MyRepository;
import model.easy.MyService;
import org.junit.jupiter.api.Test;
import ru.nsu.services.BeanContainer;
import ru.nsu.services.BeanInstanceService;
import ru.nsu.services.DependencyScanningConfig;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты на простых файлах участием JSON конфигурации и javax.injection.
 * Проверяем, что синглетон бины реально создаются один раз.
 * Prototype бины создаются каждый раз, а параметры правильно создаются, удовлетворяя конфигурации из JSON.
 */
public class EasyDataTest {

    @Test
    public void testSingletonBean() throws IOException {
        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForAnnotatedClasses("model.easy", "beans.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        MyService firstInstance = beanInstanceService.getBean("myServiceImplementation");
        MyService secondInstance = beanInstanceService.getBean("myServiceImplementation");

        assertSame(firstInstance, secondInstance, "Это синглетон бины и они должны быть всегда одинаковые");
    }

    @Test
    public void testPrototypeBean() throws IOException {
        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForAnnotatedClasses("model.easy", "beans.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        MyService serviceBean = beanInstanceService.getBean("myServiceImplementation");
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
    public void testSetters() throws IOException {

        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForAnnotatedClasses("model.easy", "beans.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        MyService myService = beanInstanceService.getBean("myServiceImplementation");
        MyRepository myRepository = beanInstanceService.getBean("myRepositoryImpl");

        assertEquals(myService.getSomeProperty(), "value1", "В значение someProperty в MyService из конфигурации установлена строка 'value1'");
        assertSame(myService.getAnotherProperty(), 5, "В значение anotherProperty в MyService из конфигурации установлено число 5");
        assertEquals(myRepository.getDataSource(), "myDataSource", "В значение myDataSource в MyRepository из конфигурации установлена строка 'myDataSource'");
    }
}
