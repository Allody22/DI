package model.real;


import lombok.AllArgsConstructor;
import lombok.Data;

import javax.inject.Named;

@Data
@AllArgsConstructor
@Named("SmsService")
public class UserDetailsCheckerService implements IUserDetails {

    @Override
    public void sendMessage(String message) {
        System.out.println("Sending SMS: " + message);
    }
}
