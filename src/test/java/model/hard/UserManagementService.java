package model.hard;

import lombok.Data;

@Data
public class UserManagementService {
    private final UserDataRepository userDataRepository;
    private final NotificationService notificationService;
    public UserManagementService(UserDataRepository userDataRepository, NotificationService notificationService){
        this.userDataRepository = userDataRepository;
        this.notificationService = notificationService;
    }
}
