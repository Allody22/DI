import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import ru.nsu.model.BeanDefinitionsWrapper;
import ru.nsu.services.DependencyContainerImp;
import ru.nsu.services.JsonBeanDefinitionReader;
import ru.nsu.services.ScanningConfig;
import ru.nsu.services.BeanControllingService;
import ru.nsu.test.threadsTest.MySingletonRepository;
import ru.nsu.test.threadsTest.MySingletonService;
import ru.nsu.test.threadsTest.MyThreadScopeRepository;

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

        BeanControllingService instantiationService =
                new BeanControllingService(dependencyContainer);
        instantiationService.instantiateAndRegisterBeans();

        MySingletonService firstSingletonService = instantiationService.getBeanByName("ru.nsu.test.threadsTest.MySingletonService");
        MySingletonService secondSingletonService = instantiationService.getBeanByName("ru.nsu.test.threadsTest.MySingletonService");

        assertSame(firstSingletonService, secondSingletonService, "Синглетон сервисы должны быть одинаковые");

        MySingletonRepository firstSingletonRepository = instantiationService.getBeanByName("ru.nsu.test.threadsTest.MySingletonRepository");
        MySingletonRepository secondSingletonRepository = instantiationService.getBeanByName("ru.nsu.test.threadsTest.MySingletonRepository");

        assertSame(firstSingletonRepository, secondSingletonRepository, "Синглетон репозитории должны быть одинаковые");
    }

    @Test
    public void differentThreadDataTest() throws IOException, InterruptedException {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("threadBeans.json");

        // Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);

        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);

        BeanControllingService instantiationService = new BeanControllingService(dependencyContainer);
        instantiationService.instantiateAndRegisterBeans();

        final int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch latch = new CountDownLatch(numberOfThreads);
        final MyThreadScopeRepository[] threadScopeBeans = new MyThreadScopeRepository[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    threadScopeBeans[threadIndex] = instantiationService.getBeanByName("ru.nsu.test.threadsTest.MyThreadScopeRepository");
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

        BeanControllingService instantiationService = new BeanControllingService(dependencyContainer);
        instantiationService.instantiateAndRegisterBeans();

        final MyThreadScopeRepository[] firstThreadBean = new MyThreadScopeRepository[1];
        final MyThreadScopeRepository[] secondThreadBean = new MyThreadScopeRepository[1];

        Thread thread1 = new Thread(() -> {
            firstThreadBean[0] = instantiationService.getBeanByName("ru.nsu.test.threadsTest.MyThreadScopeRepository");
            // Запрашиваем бин второй раз в том же потоке, чтобы проверить, что он тот же самый
            MyThreadScopeRepository sameThreadBean = instantiationService.getBeanByName("ru.nsu.test.threadsTest.MyThreadScopeRepository");
            assertSame(firstThreadBean[0], sameThreadBean, "Thread бины с одним названием должны быть одинаковые в пределах одного потока");
        });

        Thread thread2 = new Thread(() -> secondThreadBean[0] = instantiationService.getBeanByName("ru.nsu.test.threadsTest.MyThreadScopeRepository"));

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Проверяем, что экземпляры бина в разных потоках различаются
        assertNotSame(firstThreadBean[0], secondThreadBean[0], "Thread бины с одним названием должны быть разными в пределах разных потока");
    }
}