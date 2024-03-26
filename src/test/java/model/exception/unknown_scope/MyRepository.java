package model.exception.unknown_scope;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Named;

@Data
@NoArgsConstructor
@Named("myRepositoryImpl")
public class MyRepository {

    private String dataSource;

}
