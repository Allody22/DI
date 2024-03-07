import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import ru.nsu.model.BeanDefinitionsWrapper;
import ru.nsu.services.DependencyContainerImp;
import ru.nsu.services.JsonBeanDefinitionReader;
import ru.nsu.services.ScanningConfig;
import ru.nsu.services.ServicesInstantiationServiceImpl;
import ru.nsu.threadsTest.MySingletonRepository;
import ru.nsu.threadsTest.MySingletonService;
import ru.nsu.threadsTest.MyThreadScopeRepository;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

@Slf4j
public class ThreadDataTest {

    @Test
    public void testSingletonBean() throws IOException {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("threadBeans.json");

        //Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);

        ServicesInstantiationServiceImpl instantiationService =
                new ServicesInstantiationServiceImpl(dependencyContainer, scanningConfig);
        instantiationService.instantiateAndRegisterBeans();

        MySingletonService firstSingletonService = dependencyContainer.getBeanByName("ru.nsu.threadsTest.MySingletonService");
        MySingletonService secondSingletonService = dependencyContainer.getBeanByName("ru.nsu.threadsTest.MySingletonService");

        assertSame(firstSingletonService, secondSingletonService, "Синглетон сервисы должны быть одинаковые");

        MySingletonRepository firstSingletonRepository = dependencyContainer.getBeanByName("ru.nsu.threadsTest.MySingletonRepository");
        MySingletonRepository secondSingletonRepository = dependencyContainer.getBeanByName("ru.nsu.threadsTest.MySingletonRepository");

        assertSame(firstSingletonRepository, secondSingletonRepository, "Синглетон репозитории должны быть одинаковые");
    }

    @Test
    public void differentThreadDataTest() throws IOException, InterruptedException {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("threadBeans.json");

        // Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);

        ServicesInstantiationServiceImpl instantiationService = new ServicesInstantiationServiceImpl(dependencyContainer, scanningConfig);
        instantiationService.instantiateAndRegisterBeans();

        final int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch latch = new CountDownLatch(numberOfThreads);
        final MyThreadScopeRepository[] threadScopeBeans = new MyThreadScopeRepository[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    threadScopeBeans[threadIndex] = dependencyContainer.getBeanByName("ru.nsu.threadsTest.MyThreadScopeRepository");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // Ожидаем завершения всех потоков

        // Проверяем, что экземпляры различаются
        assertNotSame(threadScopeBeans[0], threadScopeBeans[1], "Бины типа thread должны быть разные в разные потоках");

        executorService.shutdown();
    }

    @Test
    public void sameThreadDataTest() throws IOException, InterruptedException {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("threadBeans.json");

        // Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);

        ServicesInstantiationServiceImpl instantiationService = new ServicesInstantiationServiceImpl(dependencyContainer, scanningConfig);
        instantiationService.instantiateAndRegisterBeans();

        final MyThreadScopeRepository[] firstThreadBean = new MyThreadScopeRepository[1];
        final MyThreadScopeRepository[] secondThreadBean = new MyThreadScopeRepository[1];

        Thread thread1 = new Thread(() -> {
            firstThreadBean[0] = dependencyContainer.getBeanByName("ru.nsu.threadsTest.MyThreadScopeRepository");
            // Запрашиваем бин второй раз в том же потоке, чтобы проверить, что он тот же самый
            MyThreadScopeRepository sameThreadBean = dependencyContainer.getBeanByName("ru.nsu.threadsTest.MyThreadScopeRepository");
            assertSame(firstThreadBean[0], sameThreadBean, "Thread бины с одним названием должны быть одинаковые в пределах одного потока");
        });

        Thread thread2 = new Thread(() -> {
            secondThreadBean[0] = dependencyContainer.getBeanByName("ru.nsu.threadsTest.MyThreadScopeRepository");
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Проверяем, что экземпляры бина в разных потоках различаются
        assertNotSame(firstThreadBean[0], secondThreadBean[0], "Thread бины с одним названием должны быть разными в пределах разных потока");
    }
}