package model.real;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@NoArgsConstructor
@Named("NotificationManager")
public class UserNewService {

    private IUserDetails smsManager;

    private IUserDetails emailManager;

    @Inject
    public UserNewService(@Named("SmsService") IUserDetails smsManager, @Named("EmailService") IUserDetails emailManager){
        this.smsManager = smsManager;
        this.emailManager = emailManager;
    }
}
