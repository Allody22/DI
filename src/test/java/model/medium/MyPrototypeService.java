package model.medium;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Named("myPrototypeService")
public class MyPrototypeService {

    private String somePrototypeProperty;

    @Inject
    @Named("myPrototypeRepository")
    private MyPrototypeRepository myPrototypeRepository;

}
