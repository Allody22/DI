package ru.nsu.test.medium;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MyPrototypeService {
    private String somePrototypeProperty;

    private final MyPrototypeRepository myPrototypeRepository;

    public MyPrototypeService(MyPrototypeRepository myPrototypeRepository){
        this.myPrototypeRepository = myPrototypeRepository;
    }
}