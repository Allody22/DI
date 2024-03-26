package tests;

import lombok.extern.slf4j.Slf4j;
import model.threads.MySingletonRepository;
import model.threads.MySingletonService;
import model.threads.MyThreadScopeRepository;
import org.junit.jupiter.api.Test;
import ru.nsu.services.BeanContainer;
import ru.nsu.services.BeanInstanceService;
import ru.nsu.services.DependencyScanningConfig;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Проверяем поведение бинов с потоковым циклом жизни.
 * Для каждого потока должен создаваться свой отдельный инстанс,
 * но внутри одного потоки эти инстансы одинаковые.
 */
@Slf4j
public class ThreadDataTest {

    @Test
    public void testSingletonBean() throws IOException {
        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForAnnotatedClasses("model.threads", "threadBeans.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        MySingletonService firstSingletonService = beanInstanceService.getBean("mySingletonService");
        MySingletonService secondSingletonService = beanInstanceService.getBean("mySingletonService");

        assertSame(firstSingletonService, secondSingletonService, "Синглетон сервисы должны быть одинаковые");

        MySingletonRepository firstSingletonRepository = beanInstanceService.getBean("mySingletonRepository");
        MySingletonRepository secondSingletonRepository = beanInstanceService.getBean("mySingletonRepository");

        assertSame(firstSingletonRepository, secondSingletonRepository, "Синглетон репозитории должны быть одинаковые");
    }

    @Test
    public void differentThreadDataTest() throws IOException, InterruptedException {
        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForAnnotatedClasses("model.threads", "threadBeans.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        final int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch latch = new CountDownLatch(numberOfThreads);
        final MyThreadScopeRepository[] threadScopeBeans = new MyThreadScopeRepository[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    threadScopeBeans[threadIndex] = beanInstanceService.getBean("myThreadScopeRepository");
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
        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForAnnotatedClasses("model.threads", "threadBeans.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        final MyThreadScopeRepository[] firstThreadBean = new MyThreadScopeRepository[1];
        final MyThreadScopeRepository[] secondThreadBean = new MyThreadScopeRepository[1];

        Thread thread1 = new Thread(() -> {
            firstThreadBean[0] = beanInstanceService.getBean("myThreadScopeRepository");
            // Запрашиваем бин второй раз в том же потоке, чтобы проверить, что он тот же самый
            MyThreadScopeRepository sameThreadBean = beanInstanceService.getBean("myThreadScopeRepository");
            assertSame(firstThreadBean[0], sameThreadBean, "Thread бины с одним названием должны быть одинаковые в пределах одного потока");
        });

        Thread thread2 = new Thread(() -> secondThreadBean[0] = beanInstanceService.getBean("myThreadScopeRepository"));

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Проверяем, что экземпляры бина в разных потоках различаются
        assertNotSame(firstThreadBean[0], secondThreadBean[0], "Thread бины с одним названием должны быть разными в пределах разных потока");
    }
}