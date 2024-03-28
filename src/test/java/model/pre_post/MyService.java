package model.pre_post;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@Named("myServiceImplementation")
public class MyService {

    private final MyRepository myRepository;

    private String someProperty;

    private Integer anotherProperty;

    private String checkConstruct = "onlyMade";

    @Inject
    public MyService(@Named("myRepositoryImpl") MyRepository myRepository) {
        this.myRepository = myRepository;
    }

    @PostConstruct
    public void init() {
        this.checkConstruct = "Service @PostConstruct";
    }

    @PreDestroy
    public void cleanup() {
        this.checkConstruct = "Service @PreDestroy";
    }
}
