package ru.nsu.test.threadsTest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MySingletonRepository {

    private String dataSource;

    private String anotherStringValue;
}
