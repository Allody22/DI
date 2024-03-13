package ru.nsu;

import lombok.extern.slf4j.Slf4j;
import ru.nsu.model.BeanDefinitionsWrapper;
import ru.nsu.services.BeanControllingService;
import ru.nsu.services.DependencyContainerImp;
import ru.nsu.services.JsonBeanDefinitionReader;
import ru.nsu.services.ScanningConfig;

import java.io.IOException;

@Slf4j
public class ThreadBeansMain {
    public static void main(String[] args) throws IOException {
        // Сначала надо прочитать json
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("threadBeans.json");

        //Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);
//        System.out.println("scanning = " + scanningConfig);
        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);
        BeanControllingService instantiationService = new BeanControllingService(dependencyContainer);

        // Инстанцируем и регистрируем бины
        instantiationService.instantiateAndRegisterBeans();
        log.info("Singleton beans = " + dependencyContainer.getSingletonInstances());
        log.info("Thread beans = " + dependencyContainer.getThreadInstances());
    }
}
