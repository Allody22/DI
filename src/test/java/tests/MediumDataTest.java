package tests;

import lombok.extern.slf4j.Slf4j;
import model.medium.MyPrototypeRepository;
import model.medium.MyPrototypeService;
import model.medium.MySingletonRepository;
import model.medium.MySingletonService;
import org.junit.jupiter.api.Test;
import ru.nsu.services.BeanContainer;
import ru.nsu.services.BeanInstanceService;
import ru.nsu.services.DependencyScanningConfig;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Проверка теста на бинах среднего размера, когда количество бинов >=4.
 * Проверяем, что синглетон бины реально создаются один раз.
 * Prototype бины создаются каждый раз, а параметры правильно создаются, удовлетворяя конфигурации из JSON.
 */
@Slf4j
public class MediumDataTest {

    @Test
    public void testPrototypeBean() throws IOException {

        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForAnnotatedClasses("model.medium", "beansMedium.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        MyPrototypeService firstPrototypeService = beanInstanceService.getBean("myPrototypeService");
        MyPrototypeService secondPrototypeService = beanInstanceService.getBean("myPrototypeService");

        assertNotNull(firstPrototypeService);
        assertNotSame(firstPrototypeService, secondPrototypeService, "Прототайпы сервисы не должны быть одинаковые");

        MyPrototypeRepository firstPrototypeRepository = beanInstanceService.getBean("myPrototypeRepository");
        MyPrototypeRepository secondPrototypeRepository = beanInstanceService.getBean("myPrototypeRepository");
        MyPrototypeRepository thirdPrototypeRepository = firstPrototypeService.getMyPrototypeRepository();
        MyPrototypeRepository fourthPrototypeRepository = secondPrototypeService.getMyPrototypeRepository();

        assertNotNull(firstPrototypeService);

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

        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForAnnotatedClasses("model.medium", "beansMedium.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        MySingletonService firstSingletonService = beanInstanceService.getBean("mySingletonService");
        MySingletonService secondSingletonService = beanInstanceService.getBean("mySingletonService");
        assertNotNull(firstSingletonService);

        assertSame(firstSingletonService, secondSingletonService, "Синглетон сервисы должны быть одинаковые");

        MySingletonRepository firstSingletonRepository = beanInstanceService.getBean("mySingletonRepository");
        MySingletonRepository secondSingletonRepository = beanInstanceService.getBean("mySingletonRepository");
        assertNotNull(firstSingletonRepository);

        assertSame(firstSingletonRepository, secondSingletonRepository, "Синглетон сервисы должны быть одинаковые");
    }

    @Test
    public void testSetters() throws IOException {

        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForAnnotatedClasses("model.medium", "beansMedium.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        MySingletonService firstSingletonService = beanInstanceService.getBean("mySingletonService");
        MySingletonService secondSingletonService = beanInstanceService.getBean("mySingletonService");
        assertNotNull(firstSingletonService);

        assertAll(
                () -> assertEquals(firstSingletonService.getSomeSingletonProperty(), secondSingletonService.getSomeSingletonProperty(), "Значение someSingletonProperty в MySingletonService должно быть 'singletonValue'"),
                () -> assertEquals(firstSingletonService.getSomeSingletonProperty(), "singletonValue", "Значение someSingletonProperty в MySingletonService должно быть 'singletonValue'"),
                () -> assertEquals(secondSingletonService.getSomeSingletonProperty(), "singletonValue", "Значение someSingletonProperty в MySingletonService должно быть 'singletonValue'")
        );


        MySingletonRepository firstSingletonRepository = beanInstanceService.getBean("mySingletonRepository");
        MySingletonRepository secondSingletonRepository = beanInstanceService.getBean("mySingletonRepository");
        assertNotNull(firstSingletonRepository);

        assertAll(
                () -> assertEquals(firstSingletonRepository.getDataSource(), secondSingletonRepository.getDataSource(), "Значение dataSource в MySingletonRepository должно быть 'singletonDataSource'"),
                () -> assertEquals(firstSingletonRepository.getDataSource(), "singletonDataSource", "Значение dataSource в MySingletonRepository должно быть 'singletonDataSource'"),
                () -> assertEquals(secondSingletonRepository.getDataSource(), "singletonDataSource", "Значение dataSource в MySingletonRepository должно быть 'singletonDataSource'")
        );

        MyPrototypeService myPrototypeService = beanInstanceService.getBean("model.medium.MyPrototypeService");
        assertEquals(myPrototypeService.getSomePrototypeProperty(), "prototypeValue", "Значение somePrototypeValue в MyPrototypeService должно быть 'prototypeValue'");

        MyPrototypeRepository myPrototypeRepository = beanInstanceService.getBean("myPrototypeRepository");
        assertEquals(myPrototypeRepository.getDataSource(), "prototypeDataSource", "Значение dataSource в MyPrototypeRepository должно быть 'prototypeDataSource'");

    }
}
