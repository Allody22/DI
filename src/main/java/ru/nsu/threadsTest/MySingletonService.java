package ru.nsu.threadsTest;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MySingletonService {

    private String someSingletonProperty;

    private Integer someIntValue;

    private final MyPrototypeRepository myPrototypeRepository;

    private final MySingletonRepository mySingletonRepository;

    private final MyThreadScopeRepository myThreadScopeRepository;

    public MySingletonService(MyPrototypeRepository myPrototypeRepository, MySingletonRepository mySingletonRepository,
                              MyThreadScopeRepository myThreadScopeRepository){
        this.myPrototypeRepository = myPrototypeRepository;
        this.mySingletonRepository = mySingletonRepository;
        this.myThreadScopeRepository = myThreadScopeRepository;
    }
}
