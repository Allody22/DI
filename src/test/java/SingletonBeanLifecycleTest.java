import org.junit.jupiter.api.Test;
import ru.nsu.config.DependencyContainerImp;
import ru.nsu.config.JsonBeanDefinitionReader;
import ru.nsu.config.ScanningConfig;
import ru.nsu.config.ServicesInstantiationServiceImpl;
import ru.nsu.model.BeanDefinitionsWrapper;
import ru.nsu.services.MyService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertSame;

public class SingletonBeanLifecycleTest {

    @Test
    public void testSingletonBean() throws IOException {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("beans.json");

        //Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp();
        ServicesInstantiationServiceImpl instantiationService =
                new ServicesInstantiationServiceImpl(dependencyContainer, scanningConfig);
        instantiationService.instantiateAndRegisterBeans();

        System.out.println("Dependency container = " + dependencyContainer.getBeanDefinitions().keySet());

        MyService firstInstance = dependencyContainer.getBeanByName("ru.nsu.services.MyService");
        MyService secondInstance = dependencyContainer.getBeanByName("ru.nsu.services.MyService");

        System.out.println("First My service " + firstInstance);
        System.out.println("Second My service " + firstInstance);
        assertSame(firstInstance, secondInstance, "Это синглетон бины и они должны быть всегда одинаковые");
    }
}
