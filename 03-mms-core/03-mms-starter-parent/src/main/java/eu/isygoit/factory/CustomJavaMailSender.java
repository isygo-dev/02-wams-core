package eu.isygoit.factory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomJavaMailSender extends JavaMailSenderImpl {

    private String defaultSender;
}
