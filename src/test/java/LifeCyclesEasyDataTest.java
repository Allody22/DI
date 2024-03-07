import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import ru.nsu.hard.MyPrototypeRepository;
import ru.nsu.hard.MyPrototypeService;
import ru.nsu.hard.MySingletonRepository;
import ru.nsu.hard.MySingletonService;
import ru.nsu.model.BeanDefinitionsWrapper;
import ru.nsu.services.DependencyContainerImp;
import ru.nsu.services.JsonBeanDefinitionReader;
import ru.nsu.services.ScanningConfig;
import ru.nsu.services.ServicesInstantiationServiceImpl;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class LifeCyclesEasyDataTest {

    @Test
    public void testPrototypeBean() throws IOException {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("beanHard.json");

        //Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);

        ServicesInstantiationServiceImpl instantiationService =
                new ServicesInstantiationServiceImpl(dependencyContainer, scanningConfig);
        instantiationService.instantiateAndRegisterBeans();

        MyPrototypeService firstPrototypeService = dependencyContainer.getBeanByName("ru.nsu.hard.MyPrototypeService");
        MyPrototypeService secondPrototypeService = dependencyContainer.getBeanByName("ru.nsu.hard.MyPrototypeService");

        assertNotSame(firstPrototypeService, secondPrototypeService, "Прототайпы сервисы не должны быть одинаковые");

        MyPrototypeRepository firstPrototypeRepository = dependencyContainer.getBeanByName("ru.nsu.hard.MyPrototypeRepository");
        MyPrototypeRepository secondPrototypeRepository = dependencyContainer.getBeanByName("ru.nsu.hard.MyPrototypeRepository");
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
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("beanHard.json");

        //Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);

        ServicesInstantiationServiceImpl instantiationService =
                new ServicesInstantiationServiceImpl(dependencyContainer, scanningConfig);
        instantiationService.instantiateAndRegisterBeans();

        MySingletonService firstSingletonService = dependencyContainer.getBeanByName("ru.nsu.hard.MySingletonService");
        MySingletonService secondSingletonService = dependencyContainer.getBeanByName("ru.nsu.hard.MySingletonService");

        assertSame(firstSingletonService, secondSingletonService, "Синглетон сервисы должны быть одинаковые");

        MySingletonRepository firstSingletonRepository = dependencyContainer.getBeanByName("ru.nsu.hard.MySingletonRepository");
        MySingletonRepository secondSingletonRepository = dependencyContainer.getBeanByName("ru.nsu.hard.MySingletonRepository");

        assertSame(firstSingletonRepository, secondSingletonRepository, "Синглетон сервисы должны быть одинаковые");
    }

    @Test
    public void testSetters() throws IOException {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("beanHard.json");

        //Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);

        ServicesInstantiationServiceImpl instantiationService =
                new ServicesInstantiationServiceImpl(dependencyContainer, scanningConfig);
        instantiationService.instantiateAndRegisterBeans();

        MySingletonService firstSingletonService = dependencyContainer.getBeanByName("ru.nsu.hard.MySingletonService");
        MySingletonService secondSingletonService = dependencyContainer.getBeanByName("ru.nsu.hard.MySingletonService");

        assertAll(
                () -> assertEquals(firstSingletonService.getSomeSingletonProperty(), secondSingletonService.getSomeSingletonProperty(), "Значение someSingletonProperty в MySingletonService должно быть 'singletonValue'"),
                () -> assertEquals(firstSingletonService.getSomeSingletonProperty(), "singletonValue", "Значение someSingletonProperty в MySingletonService должно быть 'singletonValue'"),
                () -> assertEquals(secondSingletonService.getSomeSingletonProperty(), "singletonValue", "Значение someSingletonProperty в MySingletonService должно быть 'singletonValue'")
        );


        MySingletonRepository firstSingletonRepository = dependencyContainer.getBeanByName("ru.nsu.hard.MySingletonRepository");
        MySingletonRepository secondSingletonRepository = dependencyContainer.getBeanByName("ru.nsu.hard.MySingletonRepository");

        assertAll(
                () -> assertEquals(firstSingletonRepository.getDataSource(), secondSingletonRepository.getDataSource(), "Значение dataSource в MySingletonRepository должно быть 'singletonDataSource'"),
                () -> assertEquals(firstSingletonRepository.getDataSource(), "singletonDataSource", "Значение dataSource в MySingletonRepository должно быть 'singletonDataSource'"),
                () -> assertEquals(secondSingletonRepository.getDataSource(), "singletonDataSource", "Значение dataSource в MySingletonRepository должно быть 'singletonDataSource'")
        );

        MyPrototypeService myPrototypeService = dependencyContainer.getBeanByName("ru.nsu.hard.MyPrototypeService");
        assertEquals(myPrototypeService.getSomePrototypeProperty(), "prototypeValue", "Значение somePrototypeValue в MyPrototypeService должно быть 'prototypeValue'");

        MyPrototypeRepository myPrototypeRepository = dependencyContainer.getBeanByName("ru.nsu.hard.MyPrototypeRepository");
        assertEquals(myPrototypeRepository.getDataSource(), "prototypeDataSource", "Значение dataSource в MyPrototypeRepository должно быть 'prototypeDataSource'");

    }
}
