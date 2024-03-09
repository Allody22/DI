import org.junit.jupiter.api.Test;
import ru.nsu.easy.MyRepository;
import ru.nsu.easy.MyService;
import ru.nsu.model.BeanDefinitionsWrapper;
import ru.nsu.services.DependencyContainerImp;
import ru.nsu.services.JsonBeanDefinitionReader;
import ru.nsu.services.ScanningConfig;
import ru.nsu.services.BeanControllingService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class EasyDataTest {

    @Test
    public void testSingletonBean() throws IOException {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("beans.json");

        //Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);
        BeanControllingService instantiationService =
                new BeanControllingService(dependencyContainer);
        instantiationService.instantiateAndRegisterBeans();

        MyService firstInstance = instantiationService.getBeanByName("ru.nsu.easy.MyService");
        MyService secondInstance = instantiationService.getBeanByName("ru.nsu.easy.MyService");

        assertSame(firstInstance, secondInstance, "Это синглетон бины и они должны быть всегда одинаковые");
    }

    @Test
    public void testPrototypeBean() throws IOException {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("beans.json");

        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);
        BeanControllingService instantiationService =
                new BeanControllingService(dependencyContainer);
        instantiationService.instantiateAndRegisterBeans();

        MyService serviceBean = instantiationService.getBeanByName("ru.nsu.easy.MyService");
        MyRepository firstMyRepository = instantiationService.getBeanByName("ru.nsu.easy.MyRepository");
        MyRepository secondMyRepository = instantiationService.getBeanByName("ru.nsu.easy.MyRepository");
        MyRepository thirdMyRepository = serviceBean.getMyRepository();

        assertAll("Прототайпы бинов не должны быть одинаковые",
                () -> assertNotSame(firstMyRepository, secondMyRepository),
                () -> assertNotSame(firstMyRepository, thirdMyRepository),
                () -> assertNotSame(secondMyRepository, thirdMyRepository)
        );
    }

    @Test
    public void testSetters() throws IOException {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("beans.json");

        //Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);
        BeanControllingService instantiationService =
                new BeanControllingService(dependencyContainer);
        instantiationService.instantiateAndRegisterBeans();

        MyService myService = instantiationService.getBeanByName("ru.nsu.easy.MyService");
        MyRepository myRepository = instantiationService.getBeanByName("ru.nsu.easy.MyRepository");

        assertEquals(myService.getSomeProperty(), "value1", "В значение someProperty в MyService из конфигурации установлена строка 'value1'");
        assertSame(myService.getAnotherProperty(), 5, "В значение anotherProperty в MyService из конфигурации установлено число 5");
        assertEquals(myRepository.getDataSource(), "myDataSource", "В значение myDataSource в MyRepository из конфигурации установлена строка 'myDataSource'");
    }
}
