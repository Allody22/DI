package model.json;

import lombok.Data;

import javax.inject.Provider;

@Data
public class MyService {


    private final Provider<MyRepository> myRepository;

    private String someProperty;

    private Integer anotherProperty;

    public MyService(Provider<MyRepository> myRepository) {
        this.myRepository = myRepository;
    }
}
