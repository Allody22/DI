package model.interfaces;


import lombok.AllArgsConstructor;
import lombok.Data;

import javax.inject.Named;

@Data
@AllArgsConstructor
@Named("SmsService")
public class SmsService implements IMessageService {

    @Override
    public void sendMessage(String message) {
        System.out.println("Sending SMS: " + message);
    }
}
