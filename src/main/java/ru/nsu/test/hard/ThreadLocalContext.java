package ru.nsu.test.hard;

import lombok.Data;

@Data

public class ThreadLocalContext {

    private final UserDataRepository userDataRepository;

    public ThreadLocalContext(UserDataRepository userDataRepository) {
        this.userDataRepository = userDataRepository;
    }
}
