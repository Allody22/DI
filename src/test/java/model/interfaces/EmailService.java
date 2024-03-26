package model.interfaces;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.inject.Named;

@Data
@AllArgsConstructor
@Named("EmailService")
public class EmailService implements IMessageService {

    @Override
    public void sendMessage(String message) {
        System.out.println("Sending email: " + message);
    }
}
