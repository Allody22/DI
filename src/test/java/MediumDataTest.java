import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import ru.nsu.test.medium.MyPrototypeRepository;
import ru.nsu.test.medium.MyPrototypeService;
import ru.nsu.test.medium.MySingletonRepository;
import ru.nsu.test.medium.MySingletonService;
import ru.nsu.model.BeanDefinitionsWrapper;
import ru.nsu.services.DependencyContainerImp;
import ru.nsu.services.JsonBeanDefinitionReader;
import ru.nsu.services.ScanningConfig;
import ru.nsu.services.BeanControllingService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class MediumDataTest {

    @Test
    public void testPrototypeBean() throws IOException {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("beanMedium.json");

        //Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);

        BeanControllingService instantiationService =
                new BeanControllingService(dependencyContainer);
        instantiationService.instantiateAndRegisterBeans();

        MyPrototypeService firstPrototypeService = instantiationService.getBeanByName("ru.nsu.test.medium.MyPrototypeService");
        MyPrototypeService secondPrototypeService = instantiationService.getBeanByName("ru.nsu.test.medium.MyPrototypeService");

        assertNotSame(firstPrototypeService, secondPrototypeService, "Прототайпы сервисы не должны быть одинаковые");

        MyPrototypeRepository firstPrototypeRepository = instantiationService.getBeanByName("ru.nsu.test.medium.MyPrototypeRepository");
        MyPrototypeRepository secondPrototypeRepository = instantiationService.getBeanByName("ru.nsu.test.medium.MyPrototypeRepository");
        MyPrototypeRepository thirdPrototypeRepository = firstPrototypeService.getMyPrototypeRepository();
        MyPrototypeRepository fourthPrototypeRepository = secondPrototypeService.getMyPrototypeRepository();

        assertAll(
                () -> assertNotSame(firstPrototypeRepository, secondPrototypeRepository, "Прототайп бины между первым и вторым репозиторием не совпадают"),
                () -> assertNotSame(firstPrototypeRepository, thirdPrototypeRepository, "Прототайп бины между первым и третьем репозиторием не совпадают"),
                () -> assertNotSame(firstPrototypeRepository, fourthPrototypeRepository, "Прототайп бины между первым и четвёртым репозиторием не совпадают")
        );

        assertAll(
                () -> assertNotSame(secondPrototypeRepository, thirdPrototypeRepository, "Прототайп бины между вторым и третьем репозиторием не совпадают"),
                () -> assertNotSame(secondPrototypeRepository, fourthPrototypeRepository, "Прототайп бины между вторым и четвертым репозиторием не совпадают")
        );

        assertNotSame(thirdPrototypeRepository, fourthPrototypeRepository, "Прототайп бины между третьем и четвёртым репозиторием не совпадают");
    }

    @Test
    public void testSingletonBean() throws IOException {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("beanMedium.json");

        //Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);

        BeanControllingService instantiationService =
                new BeanControllingService(dependencyContainer);
        instantiationService.instantiateAndRegisterBeans();

        MySingletonService firstSingletonService = instantiationService.getBeanByName("ru.nsu.test.medium.MySingletonService");
        MySingletonService secondSingletonService = instantiationService.getBeanByName("ru.nsu.test.medium.MySingletonService");

        assertSame(firstSingletonService, secondSingletonService, "Синглетон сервисы должны быть одинаковые");

        MySingletonRepository firstSingletonRepository = instantiationService.getBeanByName("ru.nsu.test.medium.MySingletonRepository");
        MySingletonRepository secondSingletonRepository = instantiationService.getBeanByName("ru.nsu.test.medium.MySingletonRepository");

        assertSame(firstSingletonRepository, secondSingletonRepository, "Синглетон сервисы должны быть одинаковые");
    }

    @Test
    public void testSetters() throws IOException {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("beanMedium.json");

        //Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);

        BeanControllingService instantiationService =
                new BeanControllingService(dependencyContainer);
        instantiationService.instantiateAndRegisterBeans();

        MySingletonService firstSingletonService = instantiationService.getBeanByName("ru.nsu.test.medium.MySingletonService");
        MySingletonService secondSingletonService = instantiationService.getBeanByName("ru.nsu.test.medium.MySingletonService");

        assertAll(
                () -> assertEquals(firstSingletonService.getSomeSingletonProperty(), secondSingletonService.getSomeSingletonProperty(), "Значение someSingletonProperty в MySingletonService должно быть 'singletonValue'"),
                () -> assertEquals(firstSingletonService.getSomeSingletonProperty(), "singletonValue", "Значение someSingletonProperty в MySingletonService должно быть 'singletonValue'"),
                () -> assertEquals(secondSingletonService.getSomeSingletonProperty(), "singletonValue", "Значение someSingletonProperty в MySingletonService должно быть 'singletonValue'")
        );


        MySingletonRepository firstSingletonRepository = instantiationService.getBeanByName("ru.nsu.test.medium.MySingletonRepository");
        MySingletonRepository secondSingletonRepository = instantiationService.getBeanByName("ru.nsu.test.medium.MySingletonRepository");

        assertAll(
                () -> assertEquals(firstSingletonRepository.getDataSource(), secondSingletonRepository.getDataSource(), "Значение dataSource в MySingletonRepository должно быть 'singletonDataSource'"),
                () -> assertEquals(firstSingletonRepository.getDataSource(), "singletonDataSource", "Значение dataSource в MySingletonRepository должно быть 'singletonDataSource'"),
                () -> assertEquals(secondSingletonRepository.getDataSource(), "singletonDataSource", "Значение dataSource в MySingletonRepository должно быть 'singletonDataSource'")
        );

        MyPrototypeService myPrototypeService = instantiationService.getBeanByName("ru.nsu.test.medium.MyPrototypeService");
        assertEquals(myPrototypeService.getSomePrototypeProperty(), "prototypeValue", "Значение somePrototypeValue в MyPrototypeService должно быть 'prototypeValue'");

        MyPrototypeRepository myPrototypeRepository = instantiationService.getBeanByName("ru.nsu.test.medium.MyPrototypeRepository");
        assertEquals(myPrototypeRepository.getDataSource(), "prototypeDataSource", "Значение dataSource в MyPrototypeRepository должно быть 'prototypeDataSource'");

    }
}
