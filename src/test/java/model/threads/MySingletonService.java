package model.threads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Named("mySingletonService")
public class MySingletonService {

    private String someSingletonProperty;

    private Integer someIntValue;

    @Inject
    @Named("myPrototypeRepository")
    private MyPrototypeRepository myPrototypeRepository;

    @Inject
    @Named("mySingletonRepository")
    private MySingletonRepository mySingletonRepository;

    @Inject
    @Named("myThreadScopeRepository")
    private MyThreadScopeRepository myThreadScopeRepository;
}
