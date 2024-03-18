package ru.nsu.test.medium;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MySingletonRepository {
    private String dataSource;
}
