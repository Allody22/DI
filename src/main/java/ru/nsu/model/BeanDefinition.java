package ru.nsu.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class BeanDefinition {

    private String className;

    private String name;

    private String scope;

    private Map<String, Object> initParams;

    private List<Object> constructorParams;
}
