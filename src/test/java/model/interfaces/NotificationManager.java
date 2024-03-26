package model.interfaces;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@NoArgsConstructor
@Named("NotificationManager")
public class NotificationManager {

    private IMessageService smsManager;

    private IMessageService emailManager;

    @Inject
    public NotificationManager(@Named("SmsService") IMessageService smsManager, @Named("EmailService") IMessageService emailManager){
        this.smsManager = smsManager;
        this.emailManager = emailManager;
    }
}
