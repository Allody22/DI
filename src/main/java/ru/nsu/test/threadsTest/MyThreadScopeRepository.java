package ru.nsu.test.threadsTest;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MyThreadScopeRepository {

    private String threadLocalData;

    private Integer integerThreadData;
}
