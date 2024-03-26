package model.medium;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@AllArgsConstructor
@Named("mySingletonService")
public class MySingletonService {

    private String someSingletonProperty;

    private final MyPrototypeRepository myPrototypeRepository;

    private final MySingletonRepository mySingletonRepository;

    @Inject
    public MySingletonService(@Named("myPrototypeRepository") MyPrototypeRepository myPrototypeRepository,
                              @Named("mySingletonRepository") MySingletonRepository mySingletonRepository){
        this.myPrototypeRepository = myPrototypeRepository;
        this.mySingletonRepository = mySingletonRepository;
    }
}
