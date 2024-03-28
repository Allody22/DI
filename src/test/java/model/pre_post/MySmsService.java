package model.pre_post;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

@Data
@Named("mySmsServiceImplementation")
public class MySmsService {

    private final Provider<MyRepository> myRepository;

    private String someProperty;

    private Integer anotherProperty;

    private String checkConstruct = "onlyMade";

    @Inject
    public MySmsService(@Named("myRepositoryImpl") Provider<MyRepository> myRepository) {
        this.myRepository = myRepository;
    }

    @PostConstruct
    public void init() {
        this.checkConstruct = "Service @PostConstruct";
        this.anotherProperty = 128;
    }

    @PreDestroy
    public void cleanup() {
        this.checkConstruct = "Service @PreDestroy";
        this.anotherProperty = 300;
    }
}
