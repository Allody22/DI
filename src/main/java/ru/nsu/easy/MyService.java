package ru.nsu.easy;

import lombok.Data;

@Data
public class MyService {
    private final MyRepository myRepository;

    private String someProperty;

    private Integer anotherProperty;

    public MyService(MyRepository myRepository) {
        this.myRepository = myRepository;
    }
}
