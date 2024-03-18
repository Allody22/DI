package ru.nsu;

import lombok.extern.slf4j.Slf4j;
import ru.nsu.model.BeanDefinitionsWrapper;
import ru.nsu.services.BeanControllingService;
import ru.nsu.services.DependencyContainerImp;
import ru.nsu.services.JsonBeanDefinitionReader;
import ru.nsu.services.ScanningConfig;

import java.io.IOException;

@Slf4j
public class Main {
    public static void main(String[] args) throws IOException {
        // Сначала надо прочитать json. Выбираем какой хотим
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("beans.json");

        //Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);
//        System.out.println("scanning config = " + scanningConfig);
        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);
        BeanControllingService instantiationService = new BeanControllingService(dependencyContainer);

        // Инстанцируем и регистрируем бины
        instantiationService.instantiateAndRegisterBeans();
//        var logs = dependencyContainer.getLogsForBean("ru.nsu.test.easy.MyService");

//        var logs = dependencyContainer.getLogsForBean("ru.nsu.test.easy.MyService", "logs/beans_actions.log");
        log.info("END OF THE SESSION");
//        System.out.println("logs = " + logs);
    }
}
