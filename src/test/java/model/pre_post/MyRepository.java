package model.pre_post;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Named;

@Data
@NoArgsConstructor
@Named("myRepositoryImpl")
public class MyRepository {

    private String dataSource;
    private String checkConstruct = "onlyMade";

    @PostConstruct
    public void init() {
        this.checkConstruct = "myRepo @PostConstruct";
    }

    @PreDestroy
    public void cleanup() {
        this.checkConstruct = "myRepo @PreDestroy";
    }

}
