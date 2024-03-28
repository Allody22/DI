package model.cyclic;

import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@Named("myServiceImplementation")
public class MyService {

    private final MyRepository myRepository;

    @Inject
    public MyService(@Named("myRepositoryImpl") MyRepository myRepository) {
        this.myRepository = myRepository;
    }
}
