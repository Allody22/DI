import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import ru.nsu.config.DependencyContainerImp;
import ru.nsu.config.JsonBeanDefinitionReader;
import ru.nsu.config.ScanningConfig;
import ru.nsu.config.ServicesInstantiationServiceImpl;
import ru.nsu.model.BeanDefinitionsWrapper;
import ru.nsu.repository.MyRepository;
import ru.nsu.services.MyService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotSame;

@Slf4j
public class PrototypeBeanLifecycleTest {

    @Test
    public void testPrototypeBean() throws IOException {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("beans.json");

        //Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp();
        ServicesInstantiationServiceImpl instantiationService =
                new ServicesInstantiationServiceImpl(dependencyContainer, scanningConfig);
        instantiationService.instantiateAndRegisterBeans();

//        log.info("Full dependecy container =" + dependencyContainer.toString());
        MyService serviceBean = dependencyContainer.getBeanByName("ru.nsu.services.MyService");
        var repositoryBean = serviceBean.getMyRepository();
        MyRepository secondInstance = dependencyContainer.getBeanByName("ru.nsu.repository.MyRepository");

        log.info("First repository = " + repositoryBean);
        log.info("Second repository = " + secondInstance);
        assertNotSame(repositoryBean, secondInstance, "Прототайп бины не должны быть одинаковые");
    }
}
