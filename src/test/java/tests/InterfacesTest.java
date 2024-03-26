package tests;

import model.interfaces.EmailService;
import model.interfaces.NotificationManager;
import model.interfaces.SmsService;
import org.junit.jupiter.api.Test;
import ru.nsu.services.BeanContainer;
import ru.nsu.services.BeanInstanceService;
import ru.nsu.services.DependencyScanningConfig;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Проверка того, что наши DI правильно работают в ситуациях,
 * когда один интерфейс имеет разные реализации, то есть @Named находит нужную реализацию этого интерфейса
 * и передаёт его в конструктор.
 */
public class InterfacesTest {

    @Test
    public void testInterfaces() throws IOException {
        DependencyScanningConfig dependencyScanningConfig = new DependencyScanningConfig();
        dependencyScanningConfig.scanForAnnotatedClasses("model.interfaces", "beansInterface.json");

        BeanContainer beanContainer = new BeanContainer(dependencyScanningConfig);
        BeanInstanceService beanInstanceService = new BeanInstanceService(beanContainer);
        beanInstanceService.instantiateAndRegisterBeans();

        NotificationManager notificationManager = beanInstanceService.getBean("NotificationManager");
        SmsService smsService = beanInstanceService.getBean("SmsService");
        EmailService emailService = beanInstanceService.getBean("EmailService");

        assertSame(notificationManager.getSmsManager(), smsService, "Это реализации интерфейса и интерфейс, который реализуется этим бином");
        assertSame(notificationManager.getEmailManager(), emailService, "Это реализации интерфейса и интерфейс, который реализуется этим бином");

    }

}
