package ru.nsu.test.hard;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DataSource {

    private String url;

    private String user;

    private String password;
}
