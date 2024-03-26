package model.hard;

import lombok.Data;

@Data
public class NotificationService {

    private final EmailService emailService;

    private final DataSource dataSource;

    public NotificationService(EmailService emailService, DataSource dataSource) {
        this.emailService = emailService;
        this.dataSource = dataSource;
    }
}
