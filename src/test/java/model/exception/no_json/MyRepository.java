package model.exception.no_json;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Named;

@Data
@NoArgsConstructor
@Named("myRepositoryImpl")
public class MyRepository {

    private String dataSource;

}
