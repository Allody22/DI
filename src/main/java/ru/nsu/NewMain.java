package ru.nsu;

import lombok.extern.slf4j.Slf4j;
import ru.nsu.config.DependencyContainerImp;
import ru.nsu.config.JsonBeanDefinitionReader;
import ru.nsu.config.ScanningConfig;
import ru.nsu.config.ServicesInstantiationServiceImpl;
import ru.nsu.model.BeanDefinitionsWrapper;

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
        DependencyContainerImp dependencyContainer = new DependencyContainerImp();
        ServicesInstantiationServiceImpl instantiationService = new ServicesInstantiationServiceImpl(dependencyContainer, scanningConfig);

        // Инстанцируем и регистрируем бины
        instantiationService.instantiateAndRegisterBeans();
        System.out.println("Bean definitions = " + instantiationService.getDependencyContainer().getBeanDefinitions());
    }
}
