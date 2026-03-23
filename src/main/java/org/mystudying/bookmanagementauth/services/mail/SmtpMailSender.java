package org.mystudying.bookmanagementauth.services.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.mystudying.bookmanagementauth.exceptions.RetryableMailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Profile({"prod", "test"})
public class SmtpMailSender implements MailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailSender.class);
    private final JavaMailSender javaMailSender;

    public SmtpMailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true); // true for multipart message

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true for HTML content


            log.info("Sending SMTP mail to: {}", to);
            javaMailSender.send(message);
            log.info("SMTP mail sent to: {}", to);
        } catch (MailException | MessagingException e) {
            throw new RetryableMailException("Failed to send mail to: " + to, e);
        }
    }
}
