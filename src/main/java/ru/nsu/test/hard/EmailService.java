package ru.nsu.test.hard;

import lombok.Data;

@Data
public class EmailService {
    private String config;
    private final DataSource dataSource;
    public EmailService(DataSource dataSource){
        this.dataSource = dataSource;
    }
}
