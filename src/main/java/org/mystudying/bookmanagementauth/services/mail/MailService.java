package org.mystudying.bookmanagementauth.services.mail;

import jakarta.mail.MessagingException;

public interface MailService {
    void send(String to, String subject, String body) throws MessagingException;
}
