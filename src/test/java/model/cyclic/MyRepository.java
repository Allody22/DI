package model.cyclic;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@NoArgsConstructor
@Named("myRepositoryImpl")
public class MyRepository {

    @Inject
    @Named("myServiceImplementation")
    private MyService myService;

}
