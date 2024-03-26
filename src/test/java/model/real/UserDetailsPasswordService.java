package model.real;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.inject.Named;

@Data
@AllArgsConstructor
@Named("EmailService")
public class UserDetailsPasswordService implements IUserDetails {

    @Override
    public void sendMessage(String message) {
        System.out.println("Sending email: " + message);
    }
}
