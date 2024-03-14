package ru.nsu.test.medium;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MySingletonService {

    private String someSingletonProperty;

    private final MyPrototypeRepository myPrototypeRepository;

    private final MySingletonRepository mySingletonRepository;

    public MySingletonService(MyPrototypeRepository myPrototypeRepository, MySingletonRepository mySingletonRepository){
        this.myPrototypeRepository = myPrototypeRepository;
        this.mySingletonRepository = mySingletonRepository;
    }
}
