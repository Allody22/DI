package tests;

import org.junit.jupiter.api.Test;
import ru.nsu.services.BeanContainer;
import ru.nsu.services.DependencyScanningConfig;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тесты на простых файлах участием JSON конфигурации и javax.injection.
 * Проверяем, что синглетон бины реально создаются один раз.
 * Prototype бины создаются каждый раз, а параметры правильно создаются, удовлетворяя конфигурации из JSON.
 */
public class CyclicDataTest {

    @Test
    public void testCyclic() throws IOException {
        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForAnnotatedClasses("model.cyclic", "cyclic.json");

        assertThrows(RuntimeException.class, () -> new BeanContainer(dependencyScanningConfig));
    }

}
