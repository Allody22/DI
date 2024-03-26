package model.hard;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MailConfig {

    private String host;

    private String port;

    private String username;

    private String password;
}
