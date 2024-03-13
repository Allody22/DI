package ru.nsu.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.nsu.model.BeanDefinitionsWrapper;

import java.io.IOException;
import java.io.InputStream;

@Data
@NoArgsConstructor
public class JsonBeanDefinitionReader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BeanDefinitionsWrapper readBeanDefinitions(String jsonConfigPath) throws IOException {
        InputStream jsonInput = this.getClass().getClassLoader().getResourceAsStream(jsonConfigPath);
        return objectMapper.readValue(jsonInput, BeanDefinitionsWrapper.class);
    }
}
