package tests;

import org.junit.jupiter.api.Test;
import ru.nsu.exception.ConstructorException;
import ru.nsu.exception.WrongJsonException;
import ru.nsu.services.BeanContainer;
import ru.nsu.services.BeanInstanceService;
import ru.nsu.services.DependencyScanningConfig;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Проверка возникающих наших собственных ошибок.
 * Создаётся специально плохая конфигурация, задаются неправильные параметры.
 * Проверяется конкретные выдаваемые сообщения об ошибках.
 */
public class ExceptionDataTest {

    @Test
    public void testTwoTypesOfInjection() {
        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        ConstructorException exception = assertThrows(ConstructorException.class, () -> dependencyScanningConfig.scanForAnnotatedClasses("model.exception.construct", "beansConstructError.json"));
        assertEquals("Error with bean 'model.exception.construct.MyService'.Only one type of injection is available: fields or constructors", exception.getMessage());
    }

    @Test
    public void testNoConfigForBean() {
        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        WrongJsonException exception = assertThrows(WrongJsonException.class, () -> dependencyScanningConfig.scanForAnnotatedClasses("model.exception.no_json", "beansNoConfigException.json"));
        assertEquals("Error with json for bean myRepositoryImpl. No configuration for bean with name.", exception.getMessage());

    }

    @Test
    public void testUnknownScope() throws IOException {
        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForAnnotatedClasses("model.exception.unknown_scope", "beansUnknownScopeException.json");
        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();
        assertThrows(WrongJsonException.class, () -> beanInstanceService.getBean("myServiceImplementation"));
    }
}
