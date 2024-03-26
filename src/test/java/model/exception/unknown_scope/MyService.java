package model.exception.unknown_scope;

import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

@Data
@Named("myServiceImplementation")
public class MyService {

    private final Provider<MyRepository> myRepository;

    private String someProperty;

    private Integer anotherProperty;

    @Inject
    public MyService(@Named("myRepositoryImpl") Provider<MyRepository> myRepository) {
        this.myRepository = myRepository;
    }
}
