package ru.nsu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Устанавливаем системное свойство для пути к файлу конфигурации SLF4J
        System.setProperty("logback.configurationFile", "classpath:logback.xml");
    }
}

