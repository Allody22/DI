package ru.nsu;

import lombok.extern.slf4j.Slf4j;
import ru.nsu.model.BeanDefinitionsWrapper;
import ru.nsu.services.DependencyContainerImp;
import ru.nsu.services.JsonBeanDefinitionReader;
import ru.nsu.services.ScanningConfig;
import ru.nsu.services.ServicesInstantiationServiceImpl;

import java.io.IOException;

@Slf4j
public class NewMain {
    public static void main(String[] args) throws IOException {
        // Сначала надо прочитать json
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("beans.json");

        //Сканируем информацию
        ScanningConfig scanningConfig = new ScanningConfig();
        scanningConfig.startBeanScanning(beanDefinitions);
//        System.out.println("scanning = " + scanningConfig);
        DependencyContainerImp dependencyContainer = new DependencyContainerImp(scanningConfig);
        ServicesInstantiationServiceImpl instantiationService = new ServicesInstantiationServiceImpl(dependencyContainer, scanningConfig);

        // Инстанцируем и регистрируем бины
        instantiationService.instantiateAndRegisterBeans();
        System.out.println("Dependency Container = " + instantiationService.getDependencyContainer().getSingletonInstances());
    }
}
