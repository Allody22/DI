package ru.nsu.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BeanDefinition {

    private String className;

    private String name;

    private String scope;

    private List<Field> injectedFields;

    private List<Field> injectedProviderFields;

    private Constructor<?> constructor;

    private Map<String, Object> initParams;

    private Method postConstructMethod;

    private Method preDestroyMethod;
}