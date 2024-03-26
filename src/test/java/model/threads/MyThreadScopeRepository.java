package model.threads;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Named;

@Data
@NoArgsConstructor
@Named("myThreadScopeRepository")
public class MyThreadScopeRepository {

    private String threadLocalData;

    private Integer integerThreadData;
}
