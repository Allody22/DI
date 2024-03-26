package model.medium;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Named;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Named("mySingletonRepository")
public class MySingletonRepository {

    private String dataSource;

}
