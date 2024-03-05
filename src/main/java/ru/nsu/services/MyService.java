package ru.nsu.services;

import lombok.Data;
import ru.nsu.repository.MyRepository;

@Data
public class MyService {
    private final MyRepository myRepository;

    private String someProperty;

    private Integer anotherProperty;

    public MyService(MyRepository myRepository) {
        this.myRepository = myRepository;
    }
}
