package model.medium;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Named;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Named("myPrototypeRepository")
public class MyPrototypeRepository {
    private String dataSource;
}
